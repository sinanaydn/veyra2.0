package com.veyra.auth.filter;

import com.veyra.auth.token.JwtService;
import com.veyra.auth.user.service.AuthUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Her HTTP isteğinde bir kez çalışır.
 * Authorization başlığından JWT çıkarır, doğrular ve SecurityContext'e yazar.
 * Hata durumunda sessizce geçer — Spring Security zaten 401 döner.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER   = "Authorization";

    private final JwtService jwtService;
    private final AuthUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        // Token yoksa veya Bearer ile başlamıyorsa geç
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Bozuk, süresi dolmuş veya imzası geçersiz token — SecurityContext boş kalır,
            // Spring Security korumalı endpoint'lere 401 döndürür.
            filterChain.doFilter(request, response);
            return;
        }

        // Email çıkartıldı ve henüz kimlik doğrulanmadıysa kontrol et
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
