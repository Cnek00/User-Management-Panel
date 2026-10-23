package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.Permission;
import com.example.UserManagmentPanel.entity.Scope;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.entity.UserPermission;
import com.example.UserManagmentPanel.exception.DomainRuleException;
import com.example.UserManagmentPanel.exception.DuplicateResourceException;
import com.example.UserManagmentPanel.repository.PermissionRepository;
import com.example.UserManagmentPanel.repository.ScopeRepository;
import com.example.UserManagmentPanel.repository.UserPermissionRepository;
import com.example.UserManagmentPanel.repository.UserRepository;
import com.example.UserManagmentPanel.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private ScopeRepository scopeRepository;
    @Mock
    private UserPermissionRepository userPermissionRepository;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void grantStoresWhoGrantedWhatAndForWhichScope() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100));
        User target = TestData.user(2L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50));
        Permission permission = permission("VIEW_REPORTS", false);
        Scope scope = scope("REPORT_MODULE");

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(permissionRepository.findById(5L)).thenReturn(Optional.of(permission));
        when(scopeRepository.findById(6L)).thenReturn(Optional.of(scope));
        when(authorizationService.canActOn(1L, 2L)).thenReturn(true);
        when(authorizationService.canDelegate(1L, "VIEW_REPORTS", "REPORT_MODULE")).thenReturn(true);
        when(userPermissionRepository.existsByUserIdAndPermissionIdAndScopeId(2L, 5L, 6L)).thenReturn(false);
        when(userPermissionRepository.save(any(UserPermission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserPermission grant = permissionService.grant(2L, 5L, 6L, "admin");

        assertThat(grant.getUser()).isEqualTo(target);
        assertThat(grant.getPermission()).isEqualTo(permission);
        assertThat(grant.getScope()).isEqualTo(scope);
        assertThat(grant.getGrantedBy()).isEqualTo(admin);
    }

    @Test
    void grantRejectsDuplicateScopeGrant() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100));
        User target = TestData.user(2L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50));

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(permissionRepository.findById(5L)).thenReturn(Optional.of(permission("VIEW_REPORTS", false)));
        when(scopeRepository.findById(6L)).thenReturn(Optional.of(scope("REPORT_MODULE")));
        when(authorizationService.canActOn(1L, 2L)).thenReturn(true);
        when(authorizationService.canDelegate(1L, "VIEW_REPORTS", "REPORT_MODULE")).thenReturn(true);
        when(userPermissionRepository.existsByUserIdAndPermissionIdAndScopeId(2L, 5L, 6L)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.grant(2L, 5L, 6L, "admin"))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userPermissionRepository, never()).save(any(UserPermission.class));
    }

    @Test
    void grantRejectsSelfGrant() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100));

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(permissionRepository.findById(5L)).thenReturn(Optional.of(permission("VIEW_REPORTS", false)));
        when(scopeRepository.findById(6L)).thenReturn(Optional.of(scope("REPORT_MODULE")));

        assertThatThrownBy(() -> permissionService.grant(1L, 5L, 6L, "admin"))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void grantRejectsDelegationWithoutDelegatablePermission() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100));
        User target = TestData.user(2L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50));

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(permissionRepository.findById(5L)).thenReturn(Optional.of(permission("VIEW_REPORTS", false)));
        when(scopeRepository.findById(6L)).thenReturn(Optional.of(scope("REPORT_MODULE")));
        when(authorizationService.canActOn(1L, 2L)).thenReturn(true);
        when(authorizationService.canDelegate(1L, "VIEW_REPORTS", "REPORT_MODULE")).thenReturn(false);

        assertThatThrownBy(() -> permissionService.grant(2L, 5L, 6L, "admin"))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Permission permission(String action, boolean delegatable) {
        Permission permission = new Permission();
        permission.setId(5L);
        permission.setAction(action);
        permission.setDelegatable(delegatable);
        return permission;
    }

    private Scope scope(String key) {
        Scope scope = new Scope();
        scope.setId(6L);
        scope.setScopeKey(key);
        return scope;
    }
}
