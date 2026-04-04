package com.veyra.user.mapper;

import com.veyra.user.dto.request.CreateUserRequest;
import com.veyra.user.dto.response.UserResponse;
import com.veyra.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);
}