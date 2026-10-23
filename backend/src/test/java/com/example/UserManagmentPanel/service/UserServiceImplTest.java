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
import com.example.UserManagmentPanel.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RankRepository rankRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createNormalizesInputEncodesPasswordAndAssignsDefaults() {
        User actor = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100), RoleType.ROLE_SUPER_ADMIN);
        Rank defaultRank = TestData.rank(5L, "USER", 10);
        Role defaultRole = TestData.role(RoleType.ROLE_USER);

        User form = new User();
        form.setUsername("  NewUser  ");
        form.setEmail("  New@Example.com ");
        form.setPassword("Password123!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(rankRepository.findByName("USER")).thenReturn(Optional.of(defaultRank));
        when(roleRepository.findByName(RoleType.ROLE_USER)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode("Password123!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.create(form, null, Set.of(), "admin");

        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(created.getRank()).isEqualTo(defaultRank);
        assertThat(created.getRoles()).containsExactly(defaultRole);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    void createRejectsDuplicateUsernameBeforeSaving() {
        User existing = TestData.user(2L, "newuser", "existing@example.com",
                TestData.rank(5L, "USER", 10));

        User form = new User();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setPassword("Password123!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.create(form, null, Set.of(), "admin"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("newuser");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createRejectsRankAtOrAboveActorRank() {
        User actor = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(2L, "ADMIN", 80), RoleType.ROLE_ADMIN);
        Rank adminRank = TestData.rank(2L, "ADMIN", 80);

        User form = new User();
        form.setUsername("newuser");
        form.setEmail("new@example.com");
        form.setPassword("Password123!");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(actor));
        when(rankRepository.findById(2L)).thenReturn(Optional.of(adminRank));

        assertThatThrownBy(() -> userService.create(form, 2L, Set.of(RoleType.ROLE_ADMIN), "admin"))
                .isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteRejectsSelfDeletion() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100), RoleType.ROLE_SUPER_ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.delete(1L, "admin"))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("own account");

        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void toggleRejectsSelfBlocking() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100), RoleType.ROLE_SUPER_ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.toggleEnabled(1L, "admin"))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("own account");
    }

    @Test
    void updateRejectsChangingOwnRank() {
        User admin = TestData.user(1L, "admin", "admin@example.com",
                TestData.rank(1L, "SUPER_ADMIN", 100), RoleType.ROLE_SUPER_ADMIN);
        Rank lowerRank = TestData.rank(2L, "ADMIN", 80);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(rankRepository.findById(2L)).thenReturn(Optional.of(lowerRank));

        assertThatThrownBy(() -> userService.update(1L, null, null, 2L, null, null, "admin"))
                .isInstanceOf(DomainRuleException.class)
                .hasMessageContaining("own rank");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
    }
}
