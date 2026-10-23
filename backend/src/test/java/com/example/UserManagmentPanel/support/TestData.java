package com.example.UserManagmentPanel.support;

import com.example.UserManagmentPanel.entity.Rank;
import com.example.UserManagmentPanel.entity.Role;
import com.example.UserManagmentPanel.entity.RoleType;
import com.example.UserManagmentPanel.entity.User;

public final class TestData {

    private TestData() {
    }

    public static Rank rank(long id, String name, int level) {
        return new Rank(id, name, level);
    }

    public static Role role(RoleType type) {
        return new Role((long) type.ordinal() + 1, type);
    }

    public static User user(long id, String username, String email, Rank rank, RoleType... roles) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("$2y$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234");
        user.setEnabled(true);
        user.setRank(rank);
        for (RoleType roleType : roles) {
            user.addRole(role(roleType));
        }
        return user;
    }
}
