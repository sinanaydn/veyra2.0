package com.veyra.auth.manager;

import com.veyra.auth.dto.request.LoginRequest;
import com.veyra.auth.dto.request.RegisterRequest;
import com.veyra.auth.dto.response.AuthResponse;
import com.veyra.auth.role.Role;
import com.veyra.auth.rules.AuthRules;
import com.veyra.auth.service.AuthService;
import com.veyra.auth.token.JwtService;
import com.veyra.auth.user.entity.AuthUser;
import com.veyra.auth.user.repository.AuthUserRepository;
import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.UnauthorizedException;
import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kayıt ve giriş iş akışlarını yönetir.
 *
 * register():
 *   1. AuthRules → e-posta tekrarını kontrol et
 *   2. UserService → veyra-user tablosunda profil oluştur
 *   3. AuthUser → kimlik bilgilerini kaydet (şifre BCrypt ile hash'lenir)
 *   4. JWT üret ve döndür
 *
 * login():
 *   1. E-posta ile AuthUser bul
 *   2. BCrypt ile şifre doğrula
 *   3. JWT üret ve döndür
 */
@Service
@RequiredArgsConstructor
public class AuthManager implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final UserService        userService;
    private final JwtService         jwtService;
    private final PasswordEncoder    passwordEncoder;
    private final AuthRules          authRules;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        authRules.checkIfEmailAlreadyExists(request.getEmail());

        // Profil bilgilerini veyra-user modülüne kaydettir
        var userResponse = userService.create(new CreateUserRequest(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone()
        ));

        // Kimlik doğrulama kaydını oluştur
        AuthUser authUser = AuthUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .userId(userResponse.getId())
                .build();

        authUserRepository.save(authUser);

        String token = generateToken(authUser);

        return buildResponse(authUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AuthUser authUser = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCodes.INVALID_CREDENTIALS, "E-posta veya şifre hatalı"));

        if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS, "E-posta veya şifre hatalı");
        }

        String token = generateToken(authUser);

        return buildResponse(authUser, token);
    }

    // ------------------------------------------------------------------ //
    //  Private yardımcı metotlar
    // ------------------------------------------------------------------ //

    private String generateToken(AuthUser authUser) {
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(authUser.getEmail())
                .password(authUser.getPasswordHash())
                .roles(authUser.getRole().name())
                .build();

        return jwtService.generateToken(userDetails, authUser.getUserId(), authUser.getRole());
    }

    private AuthResponse buildResponse(AuthUser authUser, String token) {
        return AuthResponse.builder()
                .token(token)
                .expiresIn(jwtExpiration)
                .role(authUser.getRole().name())
                .userId(authUser.getUserId())
                .email(authUser.getEmail())
                .build();
    }
}
