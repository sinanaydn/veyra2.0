package com.veyra.auth.rules;

import com.veyra.auth.user.repository.AuthUserRepository;
import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.AlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Auth iş kuralları — AuthManager'ı temiz tutar (SRP).
 * Her kural ihlali anında ilgili exception fırlatır.
 */
@Component
@RequiredArgsConstructor
public class AuthRules {

    private final AuthUserRepository authUserRepository;

    public void checkIfEmailAlreadyExists(String email) {
        if (authUserRepository.existsByEmail(email)) {
            throw new AlreadyExistsException(
                    ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Bu e-posta adresi zaten kayıtlı: " + email
            );
        }
    }
}
