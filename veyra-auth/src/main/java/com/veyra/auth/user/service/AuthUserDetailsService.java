package com.veyra.auth.user.service;

import com.veyra.auth.user.entity.AuthUser;
import com.veyra.auth.user.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security'nin JWT filter'ında token doğrulama sırasında çağırdığı servis.
 * E-posta ile AuthUser yükler ve Spring Security'nin UserDetails formatına dönüştürür.
 */
@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthUser authUser = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(authUser.getEmail())
                .password(authUser.getPasswordHash())
                .roles(authUser.getRole().name())   // Spring Security "ROLE_" önekini otomatik ekler
                .build();
    }
}
