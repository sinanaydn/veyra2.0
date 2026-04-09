package com.veyra.user.service;

import com.veyra.core.response.PageResponse;
import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.dto.request.UpdateUserRequest;
import com.veyra.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Kullanıcı yönetimi sözleşmesi.
 * veyra-auth modülü register sırasında create() metodunu kullanır.
 * Controller'lar ve Manager'lar bu interface'e bağlıdır — somut sınıfa değil (DIP).
 */
public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse getById(Long id);

    UserResponse getByEmail(String email);

    List<UserResponse> getAll();

    PageResponse<UserResponse> getAll(Pageable pageable);

    void delete(Long id);
}
