package com.veyra.user.manager;

import com.veyra.core.event.UserDeletedEvent;
import com.veyra.core.response.PageResponse;
import com.veyra.user.entity.User;
import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.dto.request.UpdateUserRequest;
import com.veyra.user.dto.response.UserResponse;
import com.veyra.user.mapper.UserMapper;
import com.veyra.user.repository.UserRepository;
import com.veyra.user.rules.UserRules;
import com.veyra.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManager implements UserService {

    private final UserRepository          userRepository;
    private final UserMapper               userMapper;
    private final UserRules                userRules;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        userRules.checkIfEmailAlreadyExists(request.getEmail());
        var user = userRepository.save(userMapper.toEntity(request));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        var user = userRules.getByIdOrThrow(id);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(userRules.getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        return userMapper.toResponse(userRules.getByEmailOrThrow(email));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(Pageable pageable) {
        return new PageResponse<>(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        doDelete(userRules.getByIdOrThrow(id));
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        doDelete(userRules.getByEmailOrThrow(email));
    }

    /**
     * Ortak soft-delete akışı — hem admin tarafından id ile silme hem de kullanıcının
     * kendi hesabını silmesi aynı cascade'i (AuthUser soft-delete + token revoke) tetikler.
     */
    private void doDelete(User user) {
        user.setDeleted(true);
        userRepository.save(user);
        eventPublisher.publishEvent(new UserDeletedEvent(user.getId(), user.getEmail()));
    }
}