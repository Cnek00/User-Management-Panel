package com.example.UserManagmentPanel.api;

import com.example.UserManagmentPanel.api.dto.UserResponse;
import com.example.UserManagmentPanel.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRank() != null ? user.getRank().getName() : null,
                user.getRank() != null ? user.getRank().getLevel() : 0,
                roles
        );
    }
}
