package com.example.UserManagmentPanel.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "User representation returned by the API. Never contains password data.")
public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        String rank,
        int rankLevel,
        Set<String> roles
) {
}
