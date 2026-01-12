# Git Workflow - OAuth2 Social Login

## Branch Strategy

```
main
  └── feature/oauth2-social-login
```

## Commit Plan

### Task 1: Configuration
```
feat(oauth2): add OAuth2 configuration infrastructure

- Add OAuth2Properties with @ConfigurationProperties binding
- Define nested provider configs for Google and GitHub
- Add state management configuration for CSRF protection

Enables externalized OAuth2 configuration supporting multiple
providers without code changes, following 12-factor app principles.
```

### Task 2: Enums & DTOs
```
feat(oauth2): add OAuth2 domain models and DTOs

- Add OAuth2Provider enum (GOOGLE, GITHUB)
- Add AuthProvider enum for user authentication source tracking
- Add OAuth2UserInfo record for provider-agnostic user data
- Add OAuth2TokenResponse, OAuth2ErrorResponse DTOs

Establishes type-safe domain model for OAuth2 operations,
ensuring consistent data handling across providers.
```

### Task 3: Exception Handling
```
feat(oauth2): implement OAuth2 exception hierarchy

- Add OAuth2ErrorType enum with standardized error codes
- Add OAuth2AuthenticationException base class
- Add specialized exceptions: AccountAlreadyLinkedException,
  SocialLoginRequiredException, OAuth2ProviderException
- Update GlobalExceptionHandler with OAuth2 error mapping

Provides clear, actionable error responses for OAuth2 failures,
improving debugging and user experience.
```

### Task 4: Database & Entities
```
feat(oauth2): add database support for social accounts

- Add V5__oauth2_support.sql migration
- Make User.password nullable for social-only accounts
- Add User.avatarUrl and User.authProvider fields
- Add LinkedAccount entity for provider connections
- Add LinkedAccountRepository with lookup methods

Enables users to authenticate via multiple providers while
maintaining account integrity and preventing duplicates.
```

### Task 5-6: Provider Services
```
feat(oauth2): implement OAuth2 provider services

- Add IOAuth2ProviderService interface for provider abstraction
- Implement GoogleOAuth2ProviderService with token exchange
- Implement GitHubOAuth2ProviderService with email fallback
- Add OAuth2StateService with HMAC-SHA256 state generation

Abstracts provider-specific OAuth2 flows behind common interface,
enabling easy addition of new providers (Apple, Microsoft, etc.).
```

### Task 7: Authentication Service
```
feat(oauth2): implement OAuth2 authentication service

- Add IOAuth2AuthService interface
- Implement authorization URL generation with state parameter
- Implement callback processing with token exchange
- Add findOrCreateUser logic with email-based account linking
- Add linkSocialAccount for authenticated users

Core OAuth2 business logic handling user creation, account linking,
and JWT token generation for social login users.
```

### Task 8: REST Controller
```
feat(oauth2): add OAuth2 REST endpoints

- Add GET /api/oauth2/authorize/{provider} for login initiation
- Add GET /api/oauth2/callback/{provider} for OAuth2 callback
- Add POST /api/oauth2/link/{provider} for account linking
- Update SecurityConfig with OAuth2 endpoint permissions
- Include Swagger/OpenAPI documentation

Exposes OAuth2 functionality via REST API, enabling frontend
integration with "Sign in with Google/GitHub" buttons.
```

### Task 9-10: JWT Integration
```
feat(oauth2): integrate social login with JWT system

- Update AuthServiceImpl to detect social-only users
- Throw SocialLoginRequiredException for password login attempts
- Add multi-provider login support via LinkedAccount lookup

Ensures seamless integration between OAuth2 and existing JWT
authentication, maintaining consistent user experience.
```

### Task 12-13: Tests
```
test(oauth2): add comprehensive test coverage

- Add jqwik dependency for property-based testing
- Add 12 property tests covering all correctness properties
- Add OAuth2Controller integration tests
- Add OAuth2 error handling tests

Validates OAuth2 implementation correctness across all scenarios
using property-based testing for thorough coverage.
```

## Final Merge
```
git checkout main
git merge feature/oauth2-social-login
git push origin main
```

## Rollback Plan
```
git revert <commit-hash>
# veya
git reset --hard <previous-commit>
```
