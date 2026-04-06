package com.veyra.user.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.AlreadyExistsException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.user.entity.User;
import com.veyra.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRules {

    private final UserRepository userRepository;

    public void checkIfEmailAlreadyExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AlreadyExistsException(
                    ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Bu e-posta adresi zaten kayıtlı: " + email
            );
        }
    }

    public void checkIfUserExists(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    ErrorCodes.USER_NOT_FOUND,
                    "Kullanıcı bulunamadı: " + id
            );
        }
    }

    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.USER_NOT_FOUND,
                        "Kullanıcı bulunamadı: " + email
                ));
        return user.getId();
    }
}