package com.akif.security;

import com.akif.shared.enums.Role;
import com.akif.model.User;
import com.akif.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private User adminUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();

        disabledUser = User.builder()
                .id(3L)
                .username("disableduser")
                .email("disabled@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .enabled(false)
                .build();
    }

    @Nested
    @DisplayName("Successful User Loading Tests")
    class SuccessfulUserLoadingTests {

        @Test
        @DisplayName("Should load user with USER role successfully")
        void shouldLoadUserWithUserRoleSuccessfully() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should load user with multiple roles successfully")
        void shouldLoadUserWithMultipleRolesSuccessfully() {
            when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("adminuser");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("adminuser");
            assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
            assertThat(userDetails.getAuthorities()).hasSize(2);
            assertThat(userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(java.util.stream.Collectors.toSet()))
                    .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should load disabled user correctly")
        void shouldLoadDisabledUserCorrectly() {
            when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(disabledUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("disableduser");
            assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
            assertThat(userDetails.isEnabled()).isFalse(); // User is disabled
        }

        @Test
        @DisplayName("Should load user with ADMIN role only")
        void shouldLoadUserWithAdminRoleOnly() {
            User adminOnlyUser = User.builder()
                    .id(4L)
                    .username("adminonly")
                    .email("adminonly@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.ADMIN))
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("adminonly")).thenReturn(Optional.of(adminOnlyUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("adminonly");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo("adminonly");
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("User Not Found Tests")
    class UserNotFoundTests {

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with username: nonexistent");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException for null username")
        void shouldThrowUsernameNotFoundExceptionForNullUsername() {
            when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with username: null");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException for empty username")
        void shouldThrowUsernameNotFoundExceptionForEmptyUsername() {
            when(userRepository.findByUsername("")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found with username: ");
        }
    }

    @Nested
    @DisplayName("Role Mapping Tests")
    class RoleMappingTests {

        @Test
        @DisplayName("Should map USER role to ROLE_USER authority")
        void shouldMapUserRoleToRoleUserAuthority() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        }

        @Test
        @DisplayName("Should map ADMIN role to ROLE_ADMIN authority")
        void shouldMapAdminRoleToRoleAdminAuthority() {
            User adminOnlyUser = User.builder()
                    .id(5L)
                    .username("adminonly")
                    .email("adminonly@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.ADMIN))
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("adminonly")).thenReturn(Optional.of(adminOnlyUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("adminonly");

            assertThat(userDetails.getAuthorities()).hasSize(1);
            assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should handle user with no roles")
        void shouldHandleUserWithNoRoles() {
            User noRoleUser = User.builder()
                    .id(6L)
                    .username("noroleuser")
                    .email("norole@example.com")
                    .password("encodedPassword")
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("noroleuser")).thenReturn(Optional.of(noRoleUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("noroleuser");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getAuthorities()).isEmpty();
        }
    }

    @Nested
    @DisplayName("User Account Status Tests")
    class UserAccountStatusTests {

        @Test
        @DisplayName("Should set correct account status for enabled user")
        void shouldSetCorrectAccountStatusForEnabledUser() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should set correct account status for disabled user")
        void shouldSetCorrectAccountStatusForDisabledUser() {
            when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(disabledUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

            assertThat(userDetails.isAccountNonExpired()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
            assertThat(userDetails.isEnabled()).isFalse(); // User is disabled
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle user with very long username")
        void shouldHandleUserWithVeryLongUsername() {
            String longUsername = "a".repeat(1000);
            User longUsernameUser = User.builder()
                    .id(7L)
                    .username(longUsername)
                    .email("long@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername(longUsername)).thenReturn(Optional.of(longUsernameUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername(longUsername);

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(longUsername);
        }

        @Test
        @DisplayName("Should handle user with special characters in username")
        void shouldHandleUserWithSpecialCharactersInUsername() {
            String specialUsername = "user@domain.com";
            User specialUser = User.builder()
                    .id(8L)
                    .username(specialUsername)
                    .email("special@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(specialUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername(specialUsername);

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle user with empty password")
        void shouldHandleUserWithEmptyPassword() {
            User emptyPasswordUser = User.builder()
                    .id(9L)
                    .username("emptypass")
                    .email("emptypass@example.com")
                    .password("")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            when(userRepository.findByUsername("emptypass")).thenReturn(Optional.of(emptyPasswordUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername("emptypass");

            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getPassword()).isEqualTo("");
        }
    }
}
