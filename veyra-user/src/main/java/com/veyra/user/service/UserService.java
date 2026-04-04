package com.veyra.user.service;

import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.dto.response.UserResponse;

import java.util.List;

/**
 * Kullanıcı yönetimi sözleşmesi.
 * veyra-auth modülü register sırasında create() metodunu kullanır.
 * Controller'lar ve Manager'lar bu interface'e bağlıdır — somut sınıfa değil (DIP).
 */
public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse getById(Long id);

    UserResponse getByEmail(String email);

    List<UserResponse> getAll();

    void delete(Long id);
}
