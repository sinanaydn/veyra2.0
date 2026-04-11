package com.veyra.auth.filter;

import com.veyra.core.constants.ErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * IP başına sliding-window rate limiting.
 *
 * İki bucket:
 *  - {@link Bucket#AUTH}   → /api/v1/auth/**   (5 istek / 60 sn, brute-force koruması)
 *  - {@link Bucket#PUBLIC} → GET /cars|/brands|/models/** (60 istek / 60 sn, catalog browse)
 *
 * Authenticated endpoint'ler için rate limit uygulanmaz — JWT zaten kimlik doğruluyor,
 * gerektiğinde token revoke edilerek kullanıcı engellenebilir.
 *
 * Key format: "{ip}:{bucket}" — aynı IP iki bucket'ta ayrı sayılır.
 * Bir kullanıcının /auth bucket'ını doldurması catalog browse'unu etkilemez.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int  AUTH_MAX   = 5;
    private static final int  PUBLIC_MAX = 60;
    private static final long WINDOW_MS  = 60_000;

    // Güvenilir proxy IP'leri — sadece bunlardan gelen X-Forwarded-For'a güvenilir
    private static final Set<String> TRUSTED_PROXIES = Set.of(
            "127.0.0.1", "::1", "0:0:0:0:0:0:0:1"
    );

    private final Map<String, Deque<Long>> requestCounts = new ConcurrentHashMap<>();

    /** Hangi rate-limit kovasına düşüldüğü. NONE ise filter atlanır. */
    private enum Bucket { AUTH, PUBLIC, NONE }

    // ------------------------------------------------------------------ //
    //  Filter girişi                                                      //
    // ------------------------------------------------------------------ //

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveBucket(request) == Bucket.NONE;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Bucket bucket = resolveBucket(request);
        int maxRequests = (bucket == Bucket.AUTH) ? AUTH_MAX : PUBLIC_MAX;

        String ip  = resolveClientIp(request);
        String key = ip + ":" + bucket.name();
        long now   = System.currentTimeMillis();

        Deque<Long> timestamps = requestCounts.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Pencere dışındaki eski kayıtları temizle
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String json = String.format(
                    "{\"success\":false,\"status\":429,\"message\":\"Çok fazla istek gönderildi. Lütfen bir dakika sonra tekrar deneyin.\",\"errorCode\":\"%s\",\"timestamp\":\"%s\"}",
                    ErrorCodes.RATE_LIMIT_EXCEEDED, LocalDateTime.now());

            response.getWriter().write(json);
            return;
        }

        timestamps.addLast(now);
        filterChain.doFilter(request, response);
    }

    // ------------------------------------------------------------------ //
    //  Bucket çözümü                                                      //
    // ------------------------------------------------------------------ //

    /**
     * İsteğin hangi bucket'a düştüğünü belirler.
     *
     * Öncelik sırası:
     *  1. /api/v1/auth/** → AUTH (method fark etmez — login/register/refresh hepsi)
     *  2. GET /cars|/brands|/models/** → PUBLIC (sadece GET)
     *  3. Diğer her şey → NONE (filter atlanır)
     */
    private Bucket resolveBucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/v1/auth/")) {
            return Bucket.AUTH;
        }

        if ("GET".equalsIgnoreCase(method)) {
            if (path.startsWith("/api/v1/cars")
                    || path.startsWith("/api/v1/brands")
                    || path.startsWith("/api/v1/models")) {
                return Bucket.PUBLIC;
            }
        }

        return Bucket.NONE;
    }

    // ------------------------------------------------------------------ //
    //  Client IP                                                          //
    // ------------------------------------------------------------------ //

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // X-Forwarded-For'a sadece güvenilir proxy'den geliyorsa güven
        if (TRUSTED_PROXIES.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // En sağdaki güvenilmez IP gerçek client IP'dir (rightmost-untrusted)
                String[] ips = xff.split(",");
                for (int i = ips.length - 1; i >= 0; i--) {
                    String ip = ips[i].trim();
                    if (!TRUSTED_PROXIES.contains(ip)) {
                        return ip;
                    }
                }
            }
        }
        return remoteAddr;
    }

    // ------------------------------------------------------------------ //
    //  Cleanup — 5 dakikada bir boş girdileri sil, heap sızıntısı önle
    // ------------------------------------------------------------------ //

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> {
            Deque<Long> ts = entry.getValue();
            while (!ts.isEmpty() && now - ts.peekFirst() > WINDOW_MS) {
                ts.pollFirst();
            }
            return ts.isEmpty();
        });
    }
}
