package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.RoleType;
import com.example.UserManagmentPanel.entity.User;

import java.util.List;
import java.util.Set;

public interface UserService {
    List<User> findAll();

    List<User> search(String query);

    User findById(Long id);

    User create(User user, Long rankId, Set<RoleType> roles, String actorUsername);

    User update(Long id,
                String email,
                Boolean enabled,
                Long rankId,
                Set<RoleType> roles,
                String rawPassword,
                String actorUsername);

    void updateRank(Long id, Long rankId, String actorUsername);

    void toggleEnabled(Long id, String actorUsername);

    void delete(Long id, String actorUsername);
}
