# Implementation Plan

- [x] 1. Set up project configuration





  - [x] 1.1 Create OAuth2 configuration properties class


    - Create OAuth2Properties class with @ConfigurationProperties
    - Define nested classes for Google and GitHub provider configs
    - Include clientId, clientSecret, redirectUri, authorizationUri, tokenUri, userInfoUri, scope
    - _Requirements: 5.3_

  - [x] 1.2 Update application.properties with OAuth2 placeholders

    - Add Google OAuth2 configuration properties
    - Add GitHub OAuth2 configuration properties
    - Add state secret and expiration configuration
    - _Requirements: 5.3_

- [x] 2. Create OAuth2 enums and DTOs





  - [x] 2.1 Create OAuth2Provider enum


    - Define GOOGLE and GITHUB values with string representations
    - _Requirements: 1.1, 2.1_
  - [x] 2.2 Create AuthProvider enum


    - Define LOCAL, GOOGLE, GITHUB, MIXED values
    - _Requirements: 7.5_
  - [x] 2.3 Create OAuth2UserInfo record


    - Define providerId, email, name, avatarUrl, provider fields
    - _Requirements: 1.3, 2.3_
  - [x] 2.4 Create OAuth2 request/response DTOs


    - Create OAuth2TokenResponse for provider token exchange
    - Create OAuth2ErrorResponse for error handling
    - Create LinkAccountResponseDto for account linking
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 3. Create OAuth2 exception classes




  - [x] 3.1 Create OAuth2ErrorType enum


    - Define error types: AUTHORIZATION_DENIED, AUTHENTICATION_FAILED, INVALID_TOKEN, PROVIDER_UNAVAILABLE, EMAIL_REQUIRED, INVALID_STATE, ACCOUNT_ALREADY_LINKED, SOCIAL_LOGIN_REQUIRED
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 3.2 Create OAuth2AuthenticationException and subclasses


    - Create base OAuth2AuthenticationException extending BaseException
    - Create AccountAlreadyLinkedException
    - Create SocialLoginRequiredException
    - Create OAuth2ProviderException
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  - [x] 3.3 Update GlobalExceptionHandler for OAuth2 exceptions


    - Add handler for OAuth2AuthenticationException
    - Map error types to appropriate HTTP status codes
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4. Create database migration and update User entity




  - [x] 4.1 Create V5__oauth2_support.sql migration


    - Alter users table: make password nullable, add avatar_url, add auth_provider
    - Create linked_accounts table with user_id, provider, provider_id, email, avatar_url
    - Add unique constraint on (provider, provider_id)
    - Add indexes for performance
    - Update existing users to LOCAL auth_provider
    - _Requirements: 3.4, 7.5_
  - [x] 4.2 Update User entity with OAuth2 fields


    - Make password field nullable
    - Add avatarUrl field
    - Add authProvider field with AuthProvider enum
    - Add linkedAccounts OneToMany relationship
    - _Requirements: 7.4, 7.5_
  - [x] 4.3 Create LinkedAccount entity


    - Define entity with user ManyToOne, provider, providerId, email, avatarUrl
    - Add unique constraint annotation
    - _Requirements: 3.4_
  - [x] 4.4 Create LinkedAccountRepository


    - Add findByProviderAndProviderId method
    - Add findByUserIdAndProvider method
    - Add existsByProviderAndProviderId method
    - _Requirements: 3.3, 3.5_

- [x] 5. Implement OAuth2 provider services




  - [x] 5.1 Create IOAuth2ProviderService interface


    - Define exchangeCodeForTokens method
    - Define getUserInfo method
    - Define getProviderName method
    - _Requirements: 1.2, 2.2_
  - [x] 5.2 Implement GoogleOAuth2ProviderService


    - Implement token exchange with Google's token endpoint
    - Implement user info fetching from Google's userinfo endpoint
    - Parse Google's response format
    - _Requirements: 1.2, 1.3_
  - [x] 5.3 Implement GitHubOAuth2ProviderService


    - Implement token exchange with GitHub's token endpoint
    - Implement user info fetching from GitHub's user endpoint
    - Implement email fetching from GitHub's emails endpoint when primary email is null
    - Parse GitHub's response format
    - _Requirements: 2.2, 2.3, 2.4_

- [x] 6. Implement OAuth2 state management



  - [x] 6.1 Create OAuth2StateService


    - Implement generateState method with cryptographic security
    - Implement validateState method
    - Use HMAC-SHA256 for state generation
    - Include timestamp for expiration validation
    - _Requirements: 5.1_

- [x] 7. Implement OAuth2 authentication service




  - [x] 7.1 Create IOAuth2AuthService interface


    - Define getAuthorizationUrl method
    - Define processOAuth2Callback method
    - Define linkSocialAccount method
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1_
  - [x] 7.2 Implement OAuth2AuthServiceImpl - authorization URL generation


    - Build authorization URL with client_id, redirect_uri, scope, state, response_type
    - Support both Google and GitHub providers
    - _Requirements: 1.1, 2.1_
  - [x] 7.3 Implement OAuth2AuthServiceImpl - callback processing


    - Validate state parameter
    - Exchange code for tokens via provider service
    - Extract user info from provider
    - Find or create user based on email
    - Generate JWT tokens
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 2.3, 2.5, 2.6, 2.7_

  - [x] 7.4 Implement findOrCreateUser logic

    - Check if user exists by email
    - If exists: link social account to existing user
    - If not exists: create new user with OAuth2 profile
    - Generate unique username if needed
    - Assign default USER role
    - _Requirements: 1.4, 1.5, 2.5, 2.6, 5.5, 7.1, 7.2, 7.3_
  - [x] 7.5 Implement linkSocialAccount for authenticated users


    - Verify social account not already linked to another user
    - Create LinkedAccount record
    - Update user's auth_provider if needed
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 8. Implement OAuth2 controller




  - [x] 8.1 Create OAuth2Controller

    - Implement GET /api/oauth2/authorize/{provider} endpoint
    - Implement GET /api/oauth2/callback/{provider} endpoint
    - Implement POST /api/oauth2/link/{provider} endpoint
    - Add Swagger documentation
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1_
  - [x] 8.2 Update SecurityConfig for OAuth2 endpoints


    - Add permitAll for /api/oauth2/authorize/** and /api/oauth2/callback/**
    - Add authenticated requirement for /api/oauth2/link/**
    - _Requirements: 5.1_

- [x] 9. Implement JWT integration for social login




  - [x] 9.1 Update AuthServiceImpl for social-only user handling


    - Check if user has null password and non-LOCAL auth_provider
    - Throw SocialLoginRequiredException for password login attempts
    - _Requirements: 6.4_

- [x] 10. Implement multi-provider login support



  - [x] 10.1 Update OAuth2AuthServiceImpl for multi-provider login


    - Check LinkedAccount repository for matching provider + providerId
    - Return existing user if found
    - _Requirements: 3.5_

- [x] 11. Checkpoint - Verify core implementation





  - Ensure application compiles and runs
  - Test OAuth2 flow manually with Google/GitHub credentials
  - Ask the user if questions arise

- [ ] 12. Write property-based tests
  - [ ] 12.1 Add jqwik dependency to pom.xml
    - Add jqwik 1.8.2 dependency with test scope
    - _Requirements: Testing Strategy_
  - [ ]* 12.2 Write property test for authorization URL generation
    - **Property 1: Authorization URL Generation**
    - **Validates: Requirements 1.1, 2.1**
  - [ ]* 12.3 Write property test for profile extraction
    - **Property 2: Profile Extraction from Provider Response**
    - **Validates: Requirements 1.3, 2.3**
  - [ ]* 12.4 Write property test for account linking by email match
    - **Property 3: Account Linking by Email Match**
    - **Validates: Requirements 1.4, 2.5, 3.2**
  - [ ]* 12.5 Write property test for new user creation from OAuth2
    - **Property 4: New User Creation from OAuth2**
    - **Validates: Requirements 1.5, 2.6, 7.1, 7.4, 7.5**
  - [ ]* 12.6 Write property test for JWT token generation consistency
    - **Property 5: JWT Token Generation Consistency**
    - **Validates: Requirements 1.6, 2.7, 6.1, 6.3**
  - [ ]* 12.7 Write property test for state parameter security
    - **Property 6: State Parameter Security**
    - **Validates: Requirements 5.1**
  - [ ]* 12.8 Write property test for linked account prevents duplicate linking
    - **Property 7: Linked Account Prevents Duplicate Linking**
    - **Validates: Requirements 3.3**
  - [ ]* 12.9 Write property test for multi-provider login capability
    - **Property 8: Multi-Provider Login Capability**
    - **Validates: Requirements 3.5**
  - [ ]* 12.10 Write property test for username uniqueness with suffix
    - **Property 9: Username Uniqueness with Suffix**
    - **Validates: Requirements 7.2, 7.3**
  - [ ]* 12.11 Write property test for social-only user password login rejection
    - **Property 10: Social-Only User Password Login Rejection**
    - **Validates: Requirements 6.4**
  - [ ]* 12.12 Write property test for default USER role assignment
    - **Property 11: Default USER Role Assignment**
    - **Validates: Requirements 5.5**
  - [ ]* 12.13 Write property test for refresh token works for social users
    - **Property 12: Refresh Token Works for Social Users**
    - **Validates: Requirements 6.2**

- [x] 13. Write integration and unit tests




  - [x] 13.1 Write OAuth2Controller integration tests

    - Test authorization redirect for Google and GitHub
    - Test callback with valid code and state
    - Test callback with invalid state
    - Test account linking endpoint
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1, 5.1_
  - [x] 13.2 Write OAuth2 error handling tests


    - Test authorization denied error
    - Test token exchange failure
    - Test provider unavailable error
    - Test missing email error
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 14. Final Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

  - [x] 15.1 Add OAuth2 Social Login section to README


    - Document Google and GitHub OAuth2 integration
    - Explain configuration requirements (client IDs, secrets, redirect URIs)
    - Provide example environment variables
    - Document API endpoints for OAuth2 flow
    - Include usage examples for authorization, callback, and account linking
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1_

