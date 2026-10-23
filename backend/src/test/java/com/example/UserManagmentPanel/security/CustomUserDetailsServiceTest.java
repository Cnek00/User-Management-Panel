package com.example.UserManagmentPanel.security;

import com.example.UserManagmentPanel.entity.RoleType;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.repository.UserRepository;
import com.example.UserManagmentPanel.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void mapsRolesAndRankToAuthorities() {
        User user = TestData.user(3L, "moderator", "moderator@example.com",
                TestData.rank(3L, "MODERATOR", 50), RoleType.ROLE_MODERATOR);
        when(userRepository.findByUsername("moderator")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("moderator");

        assertThat(details.getUsername()).isEqualTo("moderator");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MODERATOR", "RANK_MODERATOR");
    }

    @Test
    void disabledUserIsReportedAsDisabled() {
        User guest = TestData.user(4L, "guest", "guest@example.com",
                TestData.rank(6L, "GUEST", 1), RoleType.ROLE_USER);
        guest.setEnabled(false);
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(guest));

        UserDetails details = userDetailsService.loadUserByUsername("guest");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void unknownUsernameThrows() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
