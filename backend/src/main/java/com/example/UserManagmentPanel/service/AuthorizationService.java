package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.entity.UserPermission;
import com.example.UserManagmentPanel.exception.ResourceNotFoundException;
import com.example.UserManagmentPanel.repository.UserPermissionRepository;
import com.example.UserManagmentPanel.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Domain-level authorization rules that sit next to Spring Security's
 * role checks: a rank hierarchy, scoped permissions and delegation rules.
 */
@Service
public class AuthorizationService {

    private static final int ADMIN_LEVEL = 80;

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    public AuthorizationService(UserRepository userRepository,
                                UserPermissionRepository userPermissionRepository) {
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId, String action, String scopeKey) {
        User user = requireUser(userId);

        // Rank 80+ (ADMIN and SUPER_ADMIN) has full access.
        if (user.getRank() != null && user.getRank().getLevel() >= ADMIN_LEVEL) {
            return true;
        }

        return userPermissionRepository.findByUserId(userId).stream()
                .anyMatch(up -> matches(up, action, scopeKey));
    }

    @Transactional(readOnly = true)
    public boolean canDelegate(Long grantorId, String action, String scopeKey) {
        User grantor = requireUser(grantorId);

        if (grantor.getRank() != null && grantor.getRank().getLevel() >= ADMIN_LEVEL) {
            return true;
        }

        return userPermissionRepository.findByUserId(grantorId).stream()
                .anyMatch(up -> matches(up, action, scopeKey) && up.getPermission().isDelegatable());
    }

    /**
     * An actor may manage users strictly below their own rank.
     */
    @Transactional(readOnly = true)
    public boolean canActOn(Long actorId, Long targetId) {
        if (actorId.equals(targetId)) {
            return true;
        }

        User actor = requireUser(actorId);
        User target = requireUser(targetId);

        if (actor.getRank() == null || target.getRank() == null) {
            return false;
        }

        return actor.getRank().getLevel() > target.getRank().getLevel();
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private boolean matches(UserPermission up, String action, String scopeKey) {
        return up.getPermission().getAction().equalsIgnoreCase(action)
                && up.getScope().getScopeKey().equalsIgnoreCase(scopeKey);
    }
}
