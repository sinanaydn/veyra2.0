package com.veyra.auth.dto.request;

import com.veyra.auth.role.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    @NotNull(message = "Rol boş olamaz")
    private Role role;
}
