# Design Document: OAuth2 Social Login

## Overview

Bu tasarım, Google ve GitHub OAuth2 entegrasyonunu mevcut Spring Security + JWT tabanlı authentication sistemiyle birleştiren bir çözüm sunar. Spring Security OAuth2 Client kullanılarak Authorization Code Flow implementasyonu yapılacak, başarılı authentication sonrası mevcut JWT token sistemi üzerinden kullanıcıya token verilecektir.

### Key Design Decisions

1. **Spring Security OAuth2 Client**: Manuel OAuth2 implementasyonu yerine Spring'in built-in desteği kullanılacak
2. **Hybrid Authentication**: Hem traditional (username/password) hem social login desteklenecek
3. **Email-based Account Linking**: Aynı email'e sahip hesaplar otomatik bağlanacak
4. **JWT Continuity**: Social login sonrası da aynı JWT format ve flow kullanılacak

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Client (Frontend)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
                    ▼                 ▼                 ▼
            ┌───────────┐     ┌───────────┐     ┌───────────┐
            │  Google   │     │  GitHub   │     │ Traditional│
            │  Login    │     │  Login    │     │   Login    │
            └─────┬─────┘     └─────┬─────┘     └─────┬─────┘
                  │                 │                 │
                  └────────┬────────┴────────┬────────┘
                           │                 │
                           ▼                 ▼
              ┌────────────────────┐  ┌─────────────────┐
              │ OAuth2Controller   │  │ AuthController  │
              │ /api/oauth2/*      │  │ /api/auth/*     │
              └─────────┬──────────┘  └────────┬────────┘
                        │                      │
                        ▼                      ▼
              ┌────────────────────────────────────────┐
              │         OAuth2AuthService              │
              │  - processOAuth2Login()                │
              │  - linkSocialAccount()                 │
              │  - findOrCreateUser()                  │
              └─────────────────┬──────────────────────┘
                                │
                    ┌───────────┼───────────┐
                    │           │           │
                    ▼           ▼           ▼
            ┌───────────┐ ┌───────────┐ ┌───────────┐
            │UserRepo   │ │LinkedAcct │ │JwtToken   │
            │           │ │Repo       │ │Provider   │
            └───────────┘ └───────────┘ └───────────┘
```

### OAuth2 Authorization Code Flow

```
┌────────┐     ┌────────────┐     ┌──────────────┐     ┌────────────┐
│ User   │     │ Frontend   │     │ Backend      │     │ Provider   │
│        │     │            │     │ (Spring)     │     │ (Google/   │
│        │     │            │     │              │     │  GitHub)   │
└───┬────┘     └─────┬──────┘     └──────┬───────┘     └─────┬──────┘
    │                │                   │                   │
    │ Click Login    │                   │                   │
    │───────────────>│                   │                   │
    │                │                   │                   │
    │                │ GET /api/oauth2/  │                   │
    │                │ authorize/google  │                   │
    │                │──────────────────>│                   │
    │                │                   │                   │
    │                │   302 Redirect    │                   │
    │                │<──────────────────│                   │
    │                │                   │                   │
    │                │ Redirect to Provider                  │
    │<───────────────│──────────────────────────────────────>│
    │                │                   │                   │
    │ User Consents  │                   │                   │
    │───────────────────────────────────────────────────────>│
    │                │                   │                   │
    │                │   Callback with   │                   │
    │                │   auth code       │                   │
    │<───────────────│<──────────────────│<──────────────────│
    │                │                   │                   │
    │                │ GET /api/oauth2/  │                   │
    │                │ callback/google   │                   │
    │                │ ?code=xxx&state=y │                   │
    │                │──────────────────>│                   │
    │                │                   │                   │
    │                │                   │ Exchange code     │
    │                │                   │ for tokens        │
    │                │                   │──────────────────>│
    │                │                   │                   │
    │                │                   │ Access Token +    │
    │                │                   │ User Info         │
    │                │                   │<──────────────────│
    │                │                   │                   │
    │                │                   │ Find/Create User  │
    │                │                   │ Generate JWT      │
    │                │                   │                   │
    │                │ JWT Tokens        │                   │
    │                │<──────────────────│                   │
    │                │                   │                   │
    │ Logged In      │                   │                   │
    │<───────────────│                   │                   │
```

## Components and Interfaces

### 1. OAuth2Controller

```java
@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {
    
    // Initiates OAuth2 flow - redirects to provider
    @GetMapping("/authorize/{provider}")
    public void authorize(@PathVariable String provider, 
                         HttpServletResponse response);
    
    // Handles callback from provider
    @GetMapping("/callback/{provider}")
    public ResponseEntity<AuthResponseDto> callback(
        @PathVariable String provider,
        @RequestParam String code,
        @RequestParam String state);
    
    // Links social account to authenticated user
    @PostMapping("/link/{provider}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LinkAccountResponseDto> linkAccount(
        @PathVariable String provider,
        @RequestParam String code,
        @RequestParam String state);
}
```

### 2. IOAuth2AuthService Interface

```java
public interface IOAuth2AuthService {
    
    // Generates authorization URL with state parameter
    String getAuthorizationUrl(String provider);
    
    // Processes OAuth2 callback and returns JWT tokens
    AuthResponseDto processOAuth2Callback(String provider, 
                                          String code, 
                                          String state);
    
    // Links social account to existing user
    LinkAccountResponseDto linkSocialAccount(String provider, 
                                             String code, 
                                             String state, 
                                             Long userId);
    
    // Validates state parameter for CSRF protection
    boolean validateState(String state);
}
```

### 3. OAuth2ProviderService Interface

```java
public interface IOAuth2ProviderService {
    
    // Exchanges authorization code for tokens
    OAuth2TokenResponse exchangeCodeForTokens(String code);
    
    // Fetches user profile from provider
    OAuth2UserInfo getUserInfo(String accessToken);
    
    // Returns provider name (google, github)
    String getProviderName();
}
```

### 4. OAuth2UserInfo (Provider-agnostic user data)

```java
public record OAuth2UserInfo(
    String providerId,      // Provider's unique user ID
    String email,           // User's email
    String name,            // Display name
    String avatarUrl,       // Profile picture URL
    String provider         // "google" or "github"
) {}
```

### 5. SecurityConfig Updates

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                // Existing rules...
                .requestMatchers("/api/oauth2/authorize/**").permitAll()
                .requestMatchers("/api/oauth2/callback/**").permitAll()
                .requestMatchers("/api/oauth2/link/**").authenticated()
            )
            // JWT filter remains unchanged
            .addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class);
    }
}
```

## Data Models

### LinkedAccount Entity

```java
@Entity
@Table(name = "linked_accounts", 
       uniqueConstraints = @UniqueConstraint(
           columns = {"provider", "provider_id"}))
public class LinkedAccount extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "provider", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OAuth2Provider provider;  // GOOGLE, GITHUB
    
    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
```

### User Entity Updates

```java
@Entity
public class User extends BaseEntity {
    // Existing fields...
    
    @Column(name = "password", nullable = true)  // Now nullable for social-only users
    private String password;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(name = "auth_provider", length = 20)
    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;  // LOCAL, GOOGLE, GITHUB, MIXED
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LinkedAccount> linkedAccounts = new HashSet<>();
}
```

### OAuth2Provider Enum

```java
public enum OAuth2Provider {
    GOOGLE("google"),
    GITHUB("github");
    
    private final String value;
}
```

### AuthProvider Enum

```java
public enum AuthProvider {
    LOCAL,      // Traditional username/password only
    GOOGLE,     // Google-only (no password)
    GITHUB,     // GitHub-only (no password)
    MIXED       // Has password + linked social accounts
}
```

### Database Migration (V5__oauth2_support.sql)

```sql
-- Add OAuth2 columns to users table
ALTER TABLE users 
    ALTER COLUMN password DROP NOT NULL,
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL';

-- Create linked_accounts table
CREATE TABLE linked_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_id)
);

CREATE INDEX idx_linked_accounts_user_id ON linked_accounts(user_id);
CREATE INDEX idx_linked_accounts_provider_email ON linked_accounts(provider, email);

-- Update existing users to LOCAL auth provider
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
```

## Configuration

### application.properties

```properties
# Google OAuth2
oauth2.google.client-id=${GOOGLE_CLIENT_ID}
oauth2.google.client-secret=${GOOGLE_CLIENT_SECRET}
oauth2.google.redirect-uri=${APP_BASE_URL}/api/oauth2/callback/google
oauth2.google.authorization-uri=https://accounts.google.com/o/oauth2/v2/auth
oauth2.google.token-uri=https://oauth2.googleapis.com/token
oauth2.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
oauth2.google.scope=openid,email,profile

# GitHub OAuth2
oauth2.github.client-id=${GITHUB_CLIENT_ID}
oauth2.github.client-secret=${GITHUB_CLIENT_SECRET}
oauth2.github.redirect-uri=${APP_BASE_URL}/api/oauth2/callback/github
oauth2.github.authorization-uri=https://github.com/login/oauth/authorize
oauth2.github.token-uri=https://github.com/login/oauth/access_token
oauth2.github.user-info-uri=https://api.github.com/user
oauth2.github.user-emails-uri=https://api.github.com/user/emails
oauth2.github.scope=user:email,read:user

# OAuth2 State (CSRF protection)
oauth2.state.secret=${OAUTH2_STATE_SECRET:defaultSecretForDev}
oauth2.state.expiration-minutes=10
```



## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following properties have been consolidated to eliminate redundancy:

### Property 1: Authorization URL Generation
*For any* supported OAuth2 provider (Google, GitHub), when generating an authorization URL, the URL SHALL contain the correct authorization endpoint, client_id, redirect_uri, scope, response_type=code, and a cryptographically secure state parameter.

**Validates: Requirements 1.1, 2.1**

### Property 2: Profile Extraction from Provider Response
*For any* valid OAuth2 token response from a provider, the system SHALL extract and return a non-null OAuth2UserInfo containing providerId, email, name, and provider fields.

**Validates: Requirements 1.3, 2.3**

### Property 3: Account Linking by Email Match
*For any* OAuth2 user info with an email that matches an existing user's email, the system SHALL link the social account to that existing user rather than creating a new user.

**Validates: Requirements 1.4, 2.5, 3.2**

### Property 4: New User Creation from OAuth2
*For any* OAuth2 user info where no user with matching email exists, the system SHALL create a new user with: the provider's email as user email, null password, auth_provider set to the OAuth2 provider, and avatar_url from provider if available.

**Validates: Requirements 1.5, 2.6, 7.1, 7.4, 7.5**

### Property 5: JWT Token Generation Consistency
*For any* successful OAuth2 authentication, the generated JWT tokens SHALL have the same structure (accessToken, refreshToken, tokenType, expiresIn, username) and contain the same claims (subject=username, roles) as traditional login tokens.

**Validates: Requirements 1.6, 2.7, 6.1, 6.3**

### Property 6: State Parameter Security
*For any* OAuth2 flow, the state parameter generated during authorization SHALL be validated during callback, and any callback with an invalid or missing state SHALL be rejected.

**Validates: Requirements 5.1**

### Property 7: Linked Account Prevents Duplicate Linking
*For any* social account (provider + providerId combination) that is already linked to user A, attempting to link the same social account to user B SHALL fail with an appropriate error.

**Validates: Requirements 3.3**

### Property 8: Multi-Provider Login Capability
*For any* user with N linked social accounts (N >= 1), the user SHALL be able to successfully authenticate through any of those N providers.

**Validates: Requirements 3.5**

### Property 9: Username Uniqueness with Suffix
*For any* OAuth2 user creation where the generated username already exists, the system SHALL append a numeric suffix (1, 2, 3...) until a unique username is found.

**Validates: Requirements 7.2, 7.3**

### Property 10: Social-Only User Password Login Rejection
*For any* user with auth_provider != LOCAL and null password, attempting to login with username/password SHALL fail with an error indicating social login is required.

**Validates: Requirements 6.4**

### Property 11: Default USER Role Assignment
*For any* new user created via social login, the user SHALL be assigned the USER role by default.

**Validates: Requirements 5.5**

### Property 12: Refresh Token Works for Social Users
*For any* valid refresh token belonging to a social login user, the refresh token endpoint SHALL successfully generate new access and refresh tokens.

**Validates: Requirements 6.2**

## Error Handling

### OAuth2 Error Types

```java
public enum OAuth2ErrorType {
    AUTHORIZATION_DENIED("authorization_denied", "User denied authorization"),
    AUTHENTICATION_FAILED("authentication_failed", "Authentication failed"),
    INVALID_TOKEN("invalid_token", "Invalid or expired token"),
    PROVIDER_UNAVAILABLE("provider_unavailable", "Provider is temporarily unavailable"),
    EMAIL_REQUIRED("email_required", "Email is required but not provided"),
    INVALID_STATE("invalid_state", "Invalid state parameter - possible CSRF attack"),
    ACCOUNT_ALREADY_LINKED("account_already_linked", "This social account is already linked to another user"),
    SOCIAL_LOGIN_REQUIRED("social_login_required", "This account requires social login");
}
```

### Exception Hierarchy

```java
public class OAuth2AuthenticationException extends BaseException {
    private final OAuth2ErrorType errorType;
    private final String provider;
}

public class AccountAlreadyLinkedException extends OAuth2AuthenticationException {}
public class SocialLoginRequiredException extends OAuth2AuthenticationException {}
public class OAuth2ProviderException extends OAuth2AuthenticationException {}
```

### Error Response Format

```json
{
    "error": "authorization_denied",
    "message": "User denied authorization",
    "provider": "google",
    "timestamp": "2025-11-29T10:30:00Z"
}
```

### GlobalExceptionHandler Updates

```java
@ExceptionHandler(OAuth2AuthenticationException.class)
public ResponseEntity<OAuth2ErrorResponse> handleOAuth2Exception(
        OAuth2AuthenticationException ex) {
    
    HttpStatus status = switch (ex.getErrorType()) {
        case AUTHORIZATION_DENIED -> HttpStatus.UNAUTHORIZED;
        case INVALID_STATE -> HttpStatus.BAD_REQUEST;
        case PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        case ACCOUNT_ALREADY_LINKED -> HttpStatus.CONFLICT;
        default -> HttpStatus.UNAUTHORIZED;
    };
    
    return ResponseEntity.status(status)
        .body(new OAuth2ErrorResponse(ex.getErrorType(), ex.getMessage(), ex.getProvider()));
}
```

## Testing Strategy

### Dual Testing Approach

This feature requires both unit tests and property-based tests:
- **Unit tests**: Verify specific examples, edge cases, and error conditions
- **Property-based tests**: Verify universal properties that should hold across all inputs

### Property-Based Testing Framework

**Framework**: jqwik (Java property-based testing library)

```xml
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

### Property-Based Test Configuration

- Minimum iterations: 100 per property
- Each test annotated with: `**Feature: oauth2-social-login, Property {number}: {property_text}**`

### Test Categories

#### 1. Unit Tests
- OAuth2Controller endpoint tests (mocked service)
- OAuth2AuthService business logic tests
- Provider-specific response parsing tests
- Error handling tests for each OAuth2ErrorType
- State parameter generation and validation

#### 2. Property-Based Tests
- Authorization URL generation properties
- Profile extraction properties
- Account linking properties
- JWT consistency properties
- Username uniqueness properties

#### 3. Integration Tests
- Full OAuth2 flow with mocked provider responses
- Database integration for user creation and linking
- Security filter chain with OAuth2 endpoints

### Test Data Generators (jqwik Arbitraries)

```java
@Provide
Arbitrary<OAuth2UserInfo> oauth2UserInfos() {
    return Combinators.combine(
        Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50),  // providerId
        Arbitraries.emails(),                                            // email
        Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50),   // name
        Arbitraries.of("https://example.com/avatar.jpg", null),         // avatarUrl
        Arbitraries.of("google", "github")                              // provider
    ).as(OAuth2UserInfo::new);
}

@Provide
Arbitrary<User> existingUsers() {
    return Combinators.combine(
        Arbitraries.longs().greaterOrEqual(1),                          // id
        Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20),   // username
        Arbitraries.emails(),                                            // email
        Arbitraries.of(AuthProvider.LOCAL, AuthProvider.GOOGLE, AuthProvider.GITHUB)
    ).as((id, username, email, provider) -> {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setAuthProvider(provider);
        return user;
    });
}
```

### Key Test Scenarios

| Scenario | Test Type | Property |
|----------|-----------|----------|
| Google authorization URL generation | Property | 1 |
| GitHub authorization URL generation | Property | 1 |
| Extract profile from Google response | Property | 2 |
| Extract profile from GitHub response | Property | 2 |
| Link account when email matches | Property | 3 |
| Create new user when no email match | Property | 4 |
| JWT structure matches traditional login | Property | 5 |
| State parameter validation | Property | 6 |
| Prevent duplicate account linking | Property | 7 |
| Login via any linked provider | Property | 8 |
| Username suffix for duplicates | Property | 9 |
| Reject password login for social-only | Property | 10 |
| Default USER role for new social users | Property | 11 |
| Refresh token for social users | Property | 12 |
| User denies authorization | Unit | - |
| Token exchange failure | Unit | - |
| Provider API unavailable | Unit | - |
| Missing email from provider | Unit | - |
