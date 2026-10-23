package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.Permission;
import com.example.UserManagmentPanel.entity.Scope;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.entity.UserPermission;
import com.example.UserManagmentPanel.exception.ResourceNotFoundException;
import com.example.UserManagmentPanel.repository.UserPermissionRepository;
import com.example.UserManagmentPanel.repository.UserRepository;
import com.example.UserManagmentPanel.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPermissionRepository userPermissionRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void adminRankBypassesDirectPermissionLookup() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "ADMIN", 80));

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThat(authorizationService.hasPermission(1L, "DELETE", "SYSTEM")).isTrue();
    }

    @Test
    void regularUserNeedsMatchingDirectPermission() {
        User user = TestData.user(2L, "user", "user@example.com",
                TestData.rank(5L, "USER", 10));
        UserPermission grant = grant(user, "READ", "REPORT_MODULE", false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userPermissionRepository.findByUserId(2L)).thenReturn(List.of(grant));

        assertThat(authorizationService.hasPermission(2L, "READ", "REPORT_MODULE")).isTrue();
        assertThat(authorizationService.hasPermission(2L, "WRITE", "REPORT_MODULE")).isFalse();
        assertThat(authorizationService.hasPermission(2L, "READ", "FINANCE_MODULE")).isFalse();
    }

    @Test
    void delegationRequiresDelegatablePermission() {
        User moderator = TestData.user(3L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50));
        UserPermission locked = grant(moderator, "VIEW_REPORTS", "REPORT_MODULE", false);
        UserPermission delegatable = grant(moderator, "WRITE", "PROJECT_MODULE", true);

        when(userRepository.findById(3L)).thenReturn(Optional.of(moderator));
        when(userPermissionRepository.findByUserId(3L)).thenReturn(List.of(locked, delegatable));

        assertThat(authorizationService.canDelegate(3L, "VIEW_REPORTS", "REPORT_MODULE")).isFalse();
        assertThat(authorizationService.canDelegate(3L, "WRITE", "PROJECT_MODULE")).isTrue();
    }

    @Test
    void actorCanOnlyManageStrictlyLowerRanks() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(2L, "ADMIN", 80));
        User moderator = TestData.user(3L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50));

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(3L)).thenReturn(Optional.of(moderator));

        assertThat(authorizationService.canActOn(1L, 3L)).isTrue();
        assertThat(authorizationService.canActOn(3L, 1L)).isFalse();
        assertThat(authorizationService.canActOn(1L, 1L)).isTrue();
    }

    @Test
    void unknownUserThrowsDomainException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.hasPermission(99L, "READ", "SYSTEM"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    private UserPermission grant(User user, String action, String scopeKey, boolean delegatable) {
        Permission permission = new Permission();
        permission.setAction(action);
        permission.setDelegatable(delegatable);

        Scope scope = new Scope();
        scope.setScopeKey(scopeKey);

        UserPermission userPermission = new UserPermission();
        userPermission.setUser(user);
        userPermission.setPermission(permission);
        userPermission.setScope(scope);
        return userPermission;
    }
}
