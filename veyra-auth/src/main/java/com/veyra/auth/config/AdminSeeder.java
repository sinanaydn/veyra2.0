package com.veyra.auth.config;

import com.veyra.auth.role.Role;
import com.veyra.auth.user.entity.AuthUser;
import com.veyra.auth.user.repository.AuthUserRepository;
import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uygulama ilk başladığında varsayılan ADMIN kullanıcısını oluşturur.
 * existsByEmail kontrolü sayesinde idempotent — her restart'ta tekrar oluşturmaz.
 * Credentials application.yml'den gelir; production'da env var ile override edilir.
 */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final AuthUserRepository authUserRepository;
    private final UserService        userService;
    private final PasswordEncoder    passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (authUserRepository.existsByEmail(adminEmail)) return;

        var userResponse = userService.create(
                new CreateUserRequest("Admin", "Veyra", adminEmail, null));

        authUserRepository.save(AuthUser.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .userId(userResponse.getId())
                .build());
    }
}
