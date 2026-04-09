package com.veyra.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Ad boş bırakılamaz")
    private String firstName;

    @NotBlank(message = "Soyad boş bırakılamaz")
    private String lastName;

    private String phone;
}
