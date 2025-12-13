package com.akif.shared.security;

import com.akif.shared.security.JwtAuthenticationFilter;
import com.akif.shared.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Unit Tests")
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "tokenPrefix", "Bearer");
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "headerString", "Authorization");

        userDetails = User.builder()
                .username("testuser")
                .password("password")
                .authorities(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    @Nested
    @DisplayName("Valid JWT Token Tests")
    class ValidJwtTokenTests {

        @Test
        @DisplayName("Should set authentication for valid JWT token")
        void shouldSetAuthenticationForValidJwtToken() throws ServletException, IOException {
            String validToken = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(tokenProvider.getUsernameFromToken(validToken)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).hasSize(1);

            verify(tokenProvider).validateToken(validToken);
            verify(tokenProvider).getUsernameFromToken(validToken);
            verify(userDetailsService).loadUserByUsername("testuser");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle multiple roles in authentication")
        void shouldHandleMultipleRolesInAuthentication() throws ServletException, IOException {
            UserDetails multiRoleUser = User.builder()
                    .username("adminuser")
                    .password("password")
                    .authorities(List.of(
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),
                            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")
                    ))
                    .build();

            String validToken = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(tokenProvider.getUsernameFromToken(validToken)).thenReturn("adminuser");
            when(userDetailsService.loadUserByUsername("adminuser")).thenReturn(multiRoleUser);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("adminuser");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).hasSize(2);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Invalid JWT Token Tests")
    class InvalidJwtTokenTests {

        @Test
        @DisplayName("Should not set authentication for invalid JWT token")
        void shouldNotSetAuthenticationForInvalidJwtToken() throws ServletException, IOException {
            String invalidToken = "invalid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);
            when(tokenProvider.validateToken(invalidToken)).thenReturn(false);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

            verify(tokenProvider).validateToken(invalidToken);
            verify(tokenProvider, never()).getUsernameFromToken(anyString());
            verify(userDetailsService, never()).loadUserByUsername(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication when token validation throws exception")
        void shouldNotSetAuthenticationWhenTokenValidationThrowsException() throws ServletException, IOException {
            String token = "exception.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(tokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token validation failed"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not set authentication when user not found")
        void shouldNotSetAuthenticationWhenUserNotFound() throws ServletException, IOException {
            String validToken = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(tokenProvider.getUsernameFromToken(validToken)).thenReturn("nonexistent");
            when(userDetailsService.loadUserByUsername("nonexistent")).thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Authorization Header Tests")
    class AuthorizationHeaderTests {

        @Test
        @DisplayName("Should handle missing Authorization header")
        void shouldHandleMissingAuthorizationHeader() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle empty Authorization header")
        void shouldHandleEmptyAuthorizationHeader() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle Authorization header without Bearer prefix")
        void shouldHandleAuthorizationHeaderWithoutBearerPrefix() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6cGFzc3dvcmQ=");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle Bearer token without actual token")
        void shouldHandleBearerTokenWithoutActualToken() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should handle Bearer token with only spaces")
        void shouldHandleBearerTokenWithOnlySpaces() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Bearer   ");

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(tokenProvider, never()).validateToken(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should continue filter chain when exception occurs")
        void shouldContinueFilterChainWhenExceptionOccurs() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenThrow(new RuntimeException("Header access failed"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should continue filter chain when token provider throws exception")
        void shouldContinueFilterChainWhenTokenProviderThrowsException() throws ServletException, IOException {
            String token = "exception.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            when(tokenProvider.validateToken(token)).thenThrow(new RuntimeException("Provider exception"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should continue filter chain when user details service throws exception")
        void shouldContinueFilterChainWhenUserDetailsServiceThrowsException() throws ServletException, IOException {
            String validToken = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(tokenProvider.getUsernameFromToken(validToken)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenThrow(new RuntimeException("Service exception"));

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Security Context Tests")
    class SecurityContextTests {

        @Test
        @DisplayName("Should clear existing authentication before setting new one")
        void shouldClearExistingAuthenticationBeforeSettingNewOne() throws ServletException, IOException {
            UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken(
                    "existinguser", "password", List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            String validToken = "valid.jwt.token";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
            when(tokenProvider.validateToken(validToken)).thenReturn(true);
            when(tokenProvider.getUsernameFromToken(validToken)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isNotEqualTo("existinguser");
        }

        @Test
        @DisplayName("Should not modify security context when no valid token")
        void shouldNotModifySecurityContextWhenNoValidToken() throws ServletException, IOException {
            UsernamePasswordAuthenticationToken existingAuth = new UsernamePasswordAuthenticationToken(
                    "existinguser", "password", List.of()
            );
            SecurityContextHolder.getContext().setAuthentication(existingAuth);

            when(request.getHeader("Authorization")).thenReturn(null);

            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);
        }
    }
}
