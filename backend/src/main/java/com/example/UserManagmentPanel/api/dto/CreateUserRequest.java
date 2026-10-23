package com.example.UserManagmentPanel.api.dto;

import com.example.UserManagmentPanel.entity.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50)
        String username,

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 72)
        String password,

        @Schema(description = "Optional rank id. Defaults to USER.", example = "1")
        Long rankId,

        @Schema(description = "Optional Spring Security roles. Defaults to ROLE_USER.")
        Set<RoleType> roles
) {
}
