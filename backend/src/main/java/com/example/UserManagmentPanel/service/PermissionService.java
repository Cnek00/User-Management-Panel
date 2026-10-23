package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.Permission;
import com.example.UserManagmentPanel.entity.Scope;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.entity.UserPermission;
import com.example.UserManagmentPanel.exception.DomainRuleException;
import com.example.UserManagmentPanel.exception.DuplicateResourceException;
import com.example.UserManagmentPanel.exception.ResourceNotFoundException;
import com.example.UserManagmentPanel.repository.PermissionRepository;
import com.example.UserManagmentPanel.repository.ScopeRepository;
import com.example.UserManagmentPanel.repository.UserPermissionRepository;
import com.example.UserManagmentPanel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
public class PermissionService {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final ScopeRepository scopeRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AuthorizationService authorizationService;

    public PermissionService(UserRepository userRepository,
                             PermissionRepository permissionRepository,
                             ScopeRepository scopeRepository,
                             UserPermissionRepository userPermissionRepository,
                             AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.scopeRepository = scopeRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<UserPermission> findByUserId(Long userId) {
        return userPermissionRepository.findByUserId(userId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public UserPermission grant(Long targetUserId, Long permissionId, Long scopeId, String grantedByUsername) {
        User target = requireUser(targetUserId);
        User grantor = requireUser(grantedByUsername);
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        Scope scope = scopeRepository.findById(scopeId)
                .orElseThrow(() -> new ResourceNotFoundException("Scope not found: " + scopeId));

        if (grantor.getId().equals(target.getId())) {
            throw new DomainRuleException("You cannot grant permissions to yourself.");
        }
        if (!authorizationService.canActOn(grantor.getId(), target.getId())) {
            throw new AccessDeniedException("You cannot manage a user at the same or a higher rank.");
        }
        if (!authorizationService.canDelegate(grantor.getId(), permission.getAction(), scope.getScopeKey())) {
            throw new AccessDeniedException("You cannot delegate this permission.");
        }
        if (userPermissionRepository.existsByUserIdAndPermissionIdAndScopeId(
                target.getId(), permission.getId(), scope.getId())) {
            throw new DuplicateResourceException("This permission is already granted for the selected scope.");
        }

        UserPermission userPermission = new UserPermission();
        userPermission.setUser(target);
        userPermission.setPermission(permission);
        userPermission.setScope(scope);
        userPermission.setGrantedBy(grantor);
        return userPermissionRepository.save(userPermission);
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void revoke(Long targetUserId, Long userPermissionId, String actorUsername) {
        User actor = requireUser(actorUsername);
        UserPermission userPermission = userPermissionRepository
                .findByIdAndUserId(userPermissionId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission grant not found: " + userPermissionId));

        if (!authorizationService.canActOn(actor.getId(), targetUserId)) {
            throw new AccessDeniedException("You cannot manage a user at the same or a higher rank.");
        }

        userPermissionRepository.delete(userPermission);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
