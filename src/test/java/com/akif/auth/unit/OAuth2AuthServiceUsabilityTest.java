package com.akif.auth.unit;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.LinkedAccount;
import com.akif.auth.domain.User;
import com.akif.auth.domain.enums.OAuth2Provider;
import com.akif.auth.internal.config.OAuth2Properties;
import com.akif.auth.internal.dto.oauth2.OAuth2TokenResponse;
import com.akif.auth.internal.dto.oauth2.OAuth2UserInfo;
import com.akif.auth.internal.exception.OAuth2AuthenticationException;
import com.akif.auth.internal.repository.LinkedAccountRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.RefreshTokenService;
import com.akif.auth.internal.service.oauth2.OAuth2ProviderService;
import com.akif.auth.internal.service.oauth2.impl.OAuth2AuthServiceImpl;
import com.akif.auth.internal.service.oauth2.impl.OAuth2StateService;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OAuth2 sign-in honours account status")
class OAuth2AuthServiceUsabilityTest {

    private static final String EMAIL = "member@example.com";

    @Mock
    private OAuth2StateService stateService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LinkedAccountRepository linkedAccountRepository;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private OAuth2ProviderService providerService;

    private OAuth2AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(providerService.getProviderName()).thenReturn("google");
        when(providerService.exchangeCodeForTokens("code"))
                .thenReturn(new OAuth2TokenResponse("token", "Bearer", 3600L, null, null, null));
        when(providerService.getUserInfo("token"))
                .thenReturn(new OAuth2UserInfo("g-1", EMAIL, "Member", "Mem", "Ber", null, "google"));
        when(jwtTokenService.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtTokenService.accessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(refreshTokenService.issue(any())).thenReturn("refresh");
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneOffset.UTC);
        service = new OAuth2AuthServiceImpl(new OAuth2Properties(), stateService, userRepository,
                linkedAccountRepository, jwtTokenService, refreshTokenService, clock, List.of(providerService));
    }

    private User user() {
        User user = User.builder().username("member").email(EMAIL).enabled(true).roles(Set.of(Role.USER)).build();
        user.setId(5L);
        return user;
    }

    private void linkedTo(User user) {
        when(linkedAccountRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "g-1"))
                .thenReturn(Optional.of(LinkedAccount.builder().user(user).build()));
    }

    private void assertRejected() {
        assertThatThrownBy(() -> service.processOAuth2Callback("google", "code", "state"))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(jwtTokenService, never()).generateAccessToken(any(), any());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    @DisplayName("a banned user with a linked account cannot sign in")
    void bannedLinkedUserRejected() {
        User user = user();
        user.ban("abuse", 1L, LocalDateTime.of(2026, 11, 1, 0, 0));
        linkedTo(user);

        assertRejected();
    }

    @Test
    @DisplayName("a soft-deleted user with a linked account cannot sign in")
    void deletedLinkedUserRejected() {
        User user = user();
        user.softDelete();
        linkedTo(user);

        assertRejected();
    }

    @Test
    @DisplayName("a disabled user with a linked account cannot sign in")
    void disabledLinkedUserRejected() {
        User user = User.builder().username("member").email(EMAIL).enabled(false).roles(Set.of(Role.USER)).build();
        user.setId(5L);
        linkedTo(user);

        assertRejected();
    }

    @Test
    @DisplayName("a banned user is not auto-linked by email and no account is created")
    void bannedUserNotAutoLinked() {
        User user = user();
        user.ban("abuse", 1L, LocalDateTime.of(2026, 11, 1, 0, 0));
        when(linkedAccountRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "g-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertRejected();
        verify(linkedAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("a usable user with a linked account receives tokens")
    void usableUserSignsIn() {
        linkedTo(user());

        AuthResponse response = service.processOAuth2Callback("google", "code", "state");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }
}
