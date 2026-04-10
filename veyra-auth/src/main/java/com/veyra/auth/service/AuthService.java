package com.veyra.auth.service;

import com.veyra.auth.dto.request.ChangeRoleRequest;
import com.veyra.auth.dto.request.LoginRequest;
import com.veyra.auth.dto.request.RefreshRequest;
import com.veyra.auth.dto.request.RegisterRequest;
import com.veyra.auth.dto.response.AuthResponse;

/**
 * Kimlik doğrulama sözleşmesi — Controller bu interface'e bağlıdır (DIP).
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

    void changeRole(Long userId, ChangeRoleRequest request);
}
