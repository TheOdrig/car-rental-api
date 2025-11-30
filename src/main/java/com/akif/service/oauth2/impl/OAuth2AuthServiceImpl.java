package com.akif.service.oauth2.impl;

import com.akif.config.OAuth2Properties;
import com.akif.dto.oauth2.LinkAccountResponseDto;
import com.akif.dto.oauth2.OAuth2TokenResponse;
import com.akif.dto.oauth2.OAuth2UserInfo;
import com.akif.dto.response.AuthResponseDto;
import com.akif.enums.AuthProvider;
import com.akif.enums.OAuth2Provider;
import com.akif.enums.Role;
import com.akif.exception.AccountAlreadyLinkedException;
import com.akif.exception.OAuth2AuthenticationException;
import com.akif.enums.OAuth2ErrorType;
import com.akif.model.LinkedAccount;
import com.akif.model.User;
import com.akif.repository.LinkedAccountRepository;
import com.akif.repository.UserRepository;
import com.akif.security.JwtTokenProvider;
import com.akif.service.oauth2.IOAuth2AuthService;
import com.akif.service.oauth2.IOAuth2ProviderService;
import com.akif.service.oauth2.OAuth2StateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OAuth2AuthServiceImpl implements IOAuth2AuthService {

    private final OAuth2Properties oAuth2Properties;
    private final OAuth2StateService stateService;
    private final UserRepository userRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Map<String, IOAuth2ProviderService> providerServices;

    public OAuth2AuthServiceImpl(
            OAuth2Properties oAuth2Properties,
            OAuth2StateService stateService,
            UserRepository userRepository,
            LinkedAccountRepository linkedAccountRepository,
            JwtTokenProvider jwtTokenProvider,
            List<IOAuth2ProviderService> providerServiceList) {
        this.oAuth2Properties = oAuth2Properties;
        this.stateService = stateService;
        this.userRepository = userRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.providerServices = providerServiceList.stream()
                .collect(Collectors.toMap(IOAuth2ProviderService::getProviderName, Function.identity()));
    }


    @Override
    public String getAuthorizationUrl(String provider) {
        OAuth2Provider oAuth2Provider = OAuth2Provider.fromString(provider);
        OAuth2Properties.ProviderConfig config = getProviderConfig(oAuth2Provider);
        String state = stateService.generateState();

        String authorizationUrl = UriComponentsBuilder.fromUriString(config.getAuthorizationUri())
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("scope", config.getScope())
                .queryParam("state", state)
                .queryParam("response_type", "code")
                .build()
                .toUriString();

        log.debug("Generated authorization URL for provider {}: {}", provider, authorizationUrl);
        return authorizationUrl;
    }

    private OAuth2Properties.ProviderConfig getProviderConfig(OAuth2Provider provider) {
        return switch (provider) {
            case GOOGLE -> oAuth2Properties.getGoogle();
            case GITHUB -> oAuth2Properties.getGithub();
        };
    }

    private IOAuth2ProviderService getProviderService(String provider) {
        IOAuth2ProviderService service = providerServices.get(provider.toLowerCase());
        if (service == null) {
            log.error("No provider service found for: {}", provider);
            throw new OAuth2AuthenticationException(OAuth2ErrorType.AUTHENTICATION_FAILED, provider);
        }
        return service;
    }


    @Override
    @Transactional
    public AuthResponseDto processOAuth2Callback(String provider, String code, String state) {

        stateService.validateState(state);

        IOAuth2ProviderService providerService = getProviderService(provider);
        OAuth2TokenResponse tokenResponse = providerService.exchangeCodeForTokens(code);

        OAuth2UserInfo userInfo = providerService.getUserInfo(tokenResponse.getAccessToken());
        log.debug("Retrieved user info from {}: email={}", provider, userInfo.email());

        User user = findOrCreateUser(userInfo);

        return generateJwtTokens(user);
    }

    private AuthResponseDto generateJwtTokens(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        long expiresIn = (jwtTokenProvider.getExpirationTime(accessToken) - System.currentTimeMillis()) / 1000;

        log.info("Generated JWT tokens for user: {}", user.getUsername());

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .username(user.getUsername())
                .build();
    }


    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        OAuth2Provider oAuth2Provider = OAuth2Provider.fromString(userInfo.provider());

        Optional<LinkedAccount> existingLinkedAccount = linkedAccountRepository
                .findByProviderAndProviderId(oAuth2Provider, userInfo.providerId());

        if (existingLinkedAccount.isPresent()) {
            User user = existingLinkedAccount.get().getUser();
            log.debug("Found existing user via linked account: {}", user.getUsername());
            return user;
        }

        Optional<User> existingUser = userRepository.findByEmail(userInfo.email());

        if (existingUser.isPresent()) {

            User user = existingUser.get();
            linkSocialAccountToUser(user, userInfo, oAuth2Provider);
            log.info("Linked {} account to existing user: {}", userInfo.provider(), user.getUsername());
            return user;
        }

        User newUser = createNewUserFromOAuth2(userInfo, oAuth2Provider);
        log.info("Created new user from {} login: {}", userInfo.provider(), newUser.getUsername());
        return newUser;
    }

    private void linkSocialAccountToUser(User user, OAuth2UserInfo userInfo, OAuth2Provider provider) {
        LinkedAccount linkedAccount = LinkedAccount.builder()
                .user(user)
                .provider(provider)
                .providerId(userInfo.providerId())
                .email(userInfo.email())
                .avatarUrl(userInfo.avatarUrl())
                .build();

        linkedAccountRepository.save(linkedAccount);

        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            user.setAuthProvider(AuthProvider.MIXED);
            userRepository.save(user);
        }
    }

    private User createNewUserFromOAuth2(OAuth2UserInfo userInfo, OAuth2Provider oAuth2Provider) {
        String username = generateUniqueUsername(userInfo);

        AuthProvider authProvider = switch (oAuth2Provider) {
            case GOOGLE -> AuthProvider.GOOGLE;
            case GITHUB -> AuthProvider.GITHUB;
        };

        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);

        User newUser = User.builder()
                .username(username)
                .email(userInfo.email())
                .password(null)
                .avatarUrl(userInfo.avatarUrl())
                .authProvider(authProvider)
                .roles(roles)
                .enabled(true)
                .isDeleted(false)
                .build();

        User savedUser = userRepository.save(newUser);

        LinkedAccount linkedAccount = LinkedAccount.builder()
                .user(savedUser)
                .provider(oAuth2Provider)
                .providerId(userInfo.providerId())
                .email(userInfo.email())
                .avatarUrl(userInfo.avatarUrl())
                .build();

        linkedAccountRepository.save(linkedAccount);

        return savedUser;
    }

    private String generateUniqueUsername(OAuth2UserInfo userInfo) {

        String baseUsername = userInfo.name();
        if (baseUsername == null || baseUsername.isBlank()) {
            baseUsername = userInfo.email().split("@")[0];
        }

        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

        if (baseUsername.length() < 3) {
            baseUsername = baseUsername + "user";
        }

        if (baseUsername.length() > 20) {
            baseUsername = baseUsername.substring(0, 20);
        }

        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }


    @Override
    @Transactional
    public LinkAccountResponseDto linkSocialAccount(String provider, String code, String state, Long userId) {

        stateService.validateState(state);

        OAuth2Provider oAuth2Provider = OAuth2Provider.fromString(provider);

        IOAuth2ProviderService providerService = getProviderService(provider);
        OAuth2TokenResponse tokenResponse = providerService.exchangeCodeForTokens(code);

        OAuth2UserInfo userInfo = providerService.getUserInfo(tokenResponse.getAccessToken());
        log.debug("Retrieved user info from {} for account linking: email={}", provider, userInfo.email());

        Optional<LinkedAccount> existingLinkedAccount = linkedAccountRepository
                .findByProviderAndProviderId(oAuth2Provider, userInfo.providerId());

        if (existingLinkedAccount.isPresent()) {
            LinkedAccount linked = existingLinkedAccount.get();
            if (!linked.getUser().getId().equals(userId)) {
                log.warn("Social account {} {} is already linked to another user",
                        provider, userInfo.providerId());
                throw new AccountAlreadyLinkedException(provider);
            }

            log.debug("Social account already linked to this user");
            return buildLinkAccountResponse(provider, userInfo.email());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new OAuth2AuthenticationException(OAuth2ErrorType.AUTHENTICATION_FAILED, provider));

        LinkedAccount linkedAccount = LinkedAccount.builder()
                .user(user)
                .provider(oAuth2Provider)
                .providerId(userInfo.providerId())
                .email(userInfo.email())
                .avatarUrl(userInfo.avatarUrl())
                .build();

        linkedAccountRepository.save(linkedAccount);

        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            user.setAuthProvider(AuthProvider.MIXED);
            userRepository.save(user);
        }

        log.info("Successfully linked {} account to user {}", provider, user.getUsername());
        return buildLinkAccountResponse(provider, userInfo.email());
    }

    private LinkAccountResponseDto buildLinkAccountResponse(String provider, String email) {
        return LinkAccountResponseDto.builder()
                .message("Social account linked successfully")
                .provider(provider)
                .providerEmail(email)
                .linkedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
