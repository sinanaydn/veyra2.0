package com.veyra.user.manager;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.dto.response.UserResponse;
import com.veyra.user.mapper.UserMapper;
import com.veyra.user.repository.UserRepository;
import com.veyra.user.rules.UserRules;
import com.veyra.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManager implements UserService {

    private final UserRepository userRepository;
    private final UserMapper     userMapper;
    private final UserRules      userRules;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        userRules.checkIfEmailAlreadyExists(request.getEmail());
        var user = userRepository.save(userMapper.toEntity(request));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.USER_NOT_FOUND, "Kullanıcı bulunamadı: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.USER_NOT_FOUND, "Kullanıcı bulunamadı: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.USER_NOT_FOUND, "Kullanıcı bulunamadı: " + id));
        user.setDeleted(true);
        userRepository.save(user);
    }
}