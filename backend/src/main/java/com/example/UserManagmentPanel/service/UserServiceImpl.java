package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.Rank;
import com.example.UserManagmentPanel.entity.Role;
import com.example.UserManagmentPanel.entity.RoleType;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.exception.DomainRuleException;
import com.example.UserManagmentPanel.exception.DuplicateResourceException;
import com.example.UserManagmentPanel.exception.ResourceNotFoundException;
import com.example.UserManagmentPanel.repository.RankRepository;
import com.example.UserManagmentPanel.repository.RoleRepository;
import com.example.UserManagmentPanel.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_RANK = "USER";
    private static final int SUPER_ADMIN_LEVEL = 100;

    private final UserRepository userRepository;
    private final RankRepository rankRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;

    public UserServiceImpl(UserRepository userRepository,
                           RankRepository rankRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.rankRepository = rankRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public List<User> findAll() {
        return userRepository.findAllByOrderByRankLevelDescIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public List<User> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String term = query.trim();
        return userRepository
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByUsernameAsc(term, term);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public User create(User user, Long rankId, Set<RoleType> roles, String actorUsername) {
        String username = normalize(user.getUsername());
        String email = normalize(user.getEmail());
        ensureUnique(username, email, null);

        User actor = requireActor(actorUsername);
        Rank rank = resolveRank(rankId);
        ensureCanAssignRank(actor, rank);

        user.setUsername(username);
        user.setEmail(email);
        user.setRank(rank);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(resolveRoles(roles));
        return userRepository.save(user);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public User update(Long id,
                       String email,
                       Boolean enabled,
                       Long rankId,
                       Set<RoleType> roles,
                       String rawPassword,
                       String actorUsername) {
        User target = findById(id);
        User actor = requireActor(actorUsername);
        ensureCanManage(actor, target);

        if (email != null && !email.isBlank()) {
            String normalizedEmail = normalize(email);
            if (!normalizedEmail.equalsIgnoreCase(target.getEmail())) {
                ensureUnique(target.getUsername(), normalizedEmail, target.getId());
                target.setEmail(normalizedEmail);
            }
        }

        if (enabled != null) {
            if (target.getId().equals(actor.getId()) && !enabled) {
                throw new DomainRuleException("You cannot disable your own account.");
            }
            target.setEnabled(enabled);
        }

        if (rankId != null) {
            Rank rank = resolveRank(rankId);
            if (target.getId().equals(actor.getId()) && !target.getRank().getId().equals(rank.getId())) {
                throw new DomainRuleException("You cannot change your own rank.");
            }
            ensureCanAssignRank(actor, rank);
            target.setRank(rank);
        }

        if (roles != null && !roles.isEmpty()) {
            target.setRoles(resolveRoles(roles));
        }

        if (rawPassword != null && !rawPassword.isBlank()) {
            target.setPassword(passwordEncoder.encode(rawPassword));
        }

        return userRepository.save(target);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void updateRank(Long id, Long rankId, String actorUsername) {
        User target = findById(id);
        User actor = requireActor(actorUsername);
        ensureCanManage(actor, target);

        Rank newRank = resolveRank(rankId);
        if (target.getId().equals(actor.getId()) && !target.getRank().getId().equals(newRank.getId())) {
            throw new DomainRuleException("You cannot change your own rank.");
        }
        ensureCanAssignRank(actor, newRank);
        target.setRank(newRank);
        userRepository.save(target);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void toggleEnabled(Long id, String actorUsername) {
        User target = findById(id);
        User actor = requireActor(actorUsername);
        ensureCanManage(actor, target);

        if (target.getId().equals(actor.getId())) {
            throw new DomainRuleException("You cannot block your own account.");
        }

        target.setEnabled(!target.isEnabled());
        userRepository.save(target);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void delete(Long id, String actorUsername) {
        User target = findById(id);
        User actor = requireActor(actorUsername);
        ensureCanManage(actor, target);

        if (target.getId().equals(actor.getId())) {
            throw new DomainRuleException("You cannot delete your own account.");
        }

        userRepository.delete(target);
    }

    private void ensureUnique(String username, String email, Long currentId) {
        userRepository.findByUsername(username).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new DuplicateResourceException("Username is already in use: " + username);
            }
        });
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new DuplicateResourceException("Email is already in use: " + email);
            }
        });
    }

    private User requireActor(String actorUsername) {
        return userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + actorUsername));
    }

    private Rank resolveRank(Long rankId) {
        if (rankId == null) {
            return rankRepository.findByName(DEFAULT_RANK)
                    .orElseThrow(() -> new ResourceNotFoundException("Default rank not found: " + DEFAULT_RANK));
        }
        return rankRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found: " + rankId));
    }

    private Set<Role> resolveRoles(Set<RoleType> roleTypes) {
        Set<RoleType> requested = (roleTypes == null || roleTypes.isEmpty())
                ? Set.of(RoleType.ROLE_USER)
                : roleTypes;

        Set<Role> roles = new HashSet<>();
        for (RoleType roleType : requested) {
            roles.add(roleRepository.findByName(roleType)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleType)));
        }
        return roles;
    }

    private void ensureCanManage(User actor, User target) {
        if (actor.getId().equals(target.getId())) {
            return;
        }
        if (!authorizationService.canActOn(actor.getId(), target.getId())) {
            throw new AccessDeniedException("You cannot manage a user at the same or a higher rank.");
        }
    }

    private void ensureCanAssignRank(User actor, Rank rank) {
        if (actor.getRank() == null) {
            throw new AccessDeniedException("The acting user has no rank.");
        }
        if (actor.getRank().getLevel() >= SUPER_ADMIN_LEVEL) {
            return;
        }
        if (actor.getRank().getLevel() <= rank.getLevel()) {
            throw new AccessDeniedException("You cannot assign this rank.");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
