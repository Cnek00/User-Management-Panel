package com.example.UserManagmentPanel.api.dto;

import com.example.UserManagmentPanel.entity.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Email String email,
        Boolean enabled,
        Long rankId,
        Set<RoleType> roles,

        @Schema(description = "Optional new password. Minimum 8 characters.")
        @Size(min = 8, max = 72)
        String password
) {
}
