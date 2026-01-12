# Requirements Document

## Introduction

Bu özellik, kullanıcıların Google ve GitHub hesaplarıyla tek tıkla giriş yapabilmesini sağlayan OAuth2 Social Login entegrasyonunu tanımlar. Mevcut JWT tabanlı authentication sistemiyle entegre çalışacak, kullanıcı deneyimini iyileştirecek ve güvenliği artıracaktır. Kullanıcılar hem geleneksel email/password hem de social login ile sisteme erişebilecek, mevcut hesaplarını social account'larla bağlayabilecektir.

## Glossary

- **OAuth2**: Üçüncü taraf uygulamaların kullanıcı adına kaynaklara erişmesini sağlayan yetkilendirme protokolü
- **Authorization Code Flow**: OAuth2'nin en güvenli akışı; authorization code alınır, sonra token ile değiştirilir
- **Social Provider**: Google, GitHub gibi OAuth2 kimlik sağlayıcıları
- **Account Linking**: Mevcut bir kullanıcı hesabını social provider hesabıyla ilişkilendirme işlemi
- **JWT (JSON Web Token)**: Kullanıcı kimlik bilgilerini taşıyan, imzalanmış token formatı
- **Access Token**: Kısa ömürlü, API erişimi için kullanılan token
- **Refresh Token**: Uzun ömürlü, yeni access token almak için kullanılan token
- **Provider ID**: Social provider'ın kullanıcıya atadığı benzersiz kimlik (Google sub, GitHub id)
- **Rent-a-Car System**: Araç kiralama işlemlerini yöneten ana uygulama

## Requirements

### Requirement 1: Google OAuth2 Login

**User Story:** As a user, I want to sign in with my Google account, so that I can access the system without creating a new password.

#### Acceptance Criteria

1. WHEN a user clicks the "Sign in with Google" button THEN the Rent-a-Car System SHALL redirect the user to Google's authorization page
2. WHEN Google authorization succeeds THEN the Rent-a-Car System SHALL receive an authorization code and exchange it for tokens
3. WHEN the Rent-a-Car System receives valid Google tokens THEN the Rent-a-Car System SHALL extract user profile information (email, name, avatar URL, provider ID)
4. WHEN a user with the same email exists in the database THEN the Rent-a-Car System SHALL link the Google account to the existing user
5. WHEN no user with the email exists THEN the Rent-a-Car System SHALL create a new user account with the Google profile information
6. WHEN Google authentication completes successfully THEN the Rent-a-Car System SHALL issue JWT access and refresh tokens to the user

### Requirement 2: GitHub OAuth2 Login

**User Story:** As a developer user, I want to sign in with my GitHub account, so that I can quickly access the system using my existing developer identity.

#### Acceptance Criteria

1. WHEN a user clicks the "Sign in with GitHub" button THEN the Rent-a-Car System SHALL redirect the user to GitHub's authorization page
2. WHEN GitHub authorization succeeds THEN the Rent-a-Car System SHALL receive an authorization code and exchange it for tokens
3. WHEN the Rent-a-Car System receives valid GitHub tokens THEN the Rent-a-Car System SHALL extract user profile information (email, name, avatar URL, provider ID)
4. WHEN GitHub does not provide a public email THEN the Rent-a-Car System SHALL fetch the user's primary email from GitHub's email API
5. WHEN a user with the same email exists in the database THEN the Rent-a-Car System SHALL link the GitHub account to the existing user
6. WHEN no user with the email exists THEN the Rent-a-Car System SHALL create a new user account with the GitHub profile information
7. WHEN GitHub authentication completes successfully THEN the Rent-a-Car System SHALL issue JWT access and refresh tokens to the user

### Requirement 3: Account Linking

**User Story:** As an existing user, I want to link my social accounts to my existing account, so that I can use multiple login methods.

#### Acceptance Criteria

1. WHEN an authenticated user initiates social account linking THEN the Rent-a-Car System SHALL redirect to the provider's authorization page
2. WHEN the social account email matches the authenticated user's email THEN the Rent-a-Car System SHALL link the social account to the user
3. WHEN the social account is already linked to another user THEN the Rent-a-Car System SHALL reject the linking request and return an error message
4. WHEN account linking succeeds THEN the Rent-a-Car System SHALL store the provider name and provider ID in the user's linked accounts
5. WHEN a user has multiple linked accounts THEN the Rent-a-Car System SHALL allow login through any linked provider

### Requirement 4: OAuth2 Error Handling

**User Story:** As a user, I want clear error messages when social login fails, so that I can understand what went wrong and take corrective action.

#### Acceptance Criteria

1. IF the user denies authorization at the provider THEN the Rent-a-Car System SHALL redirect to the login page with an "authorization_denied" error message
2. IF the authorization code exchange fails THEN the Rent-a-Car System SHALL log the error details and return a generic "authentication_failed" message to the user
3. IF the provider's token is invalid or expired THEN the Rent-a-Car System SHALL return an "invalid_token" error and prompt re-authentication
4. IF the provider API is unavailable THEN the Rent-a-Car System SHALL return a "provider_unavailable" error and suggest trying again later
5. IF email is required but not provided by the provider THEN the Rent-a-Car System SHALL return an "email_required" error message

### Requirement 5: Security Requirements

**User Story:** As a system administrator, I want OAuth2 implementation to follow security best practices, so that user data remains protected.

#### Acceptance Criteria

1. WHEN initiating OAuth2 flow THEN the Rent-a-Car System SHALL generate and validate a cryptographically secure state parameter to prevent CSRF attacks
2. WHEN exchanging authorization code for tokens THEN the Rent-a-Car System SHALL use HTTPS for all provider communication
3. WHEN storing OAuth2 credentials THEN the Rent-a-Car System SHALL store client secrets in environment variables, not in source code
4. WHEN receiving tokens from providers THEN the Rent-a-Car System SHALL validate token signatures before extracting user information
5. WHEN a social login creates a new user THEN the Rent-a-Car System SHALL assign the default USER role

### Requirement 6: JWT Integration

**User Story:** As a user, I want social login to work seamlessly with the existing authentication system, so that I have a consistent experience regardless of login method.

#### Acceptance Criteria

1. WHEN social authentication succeeds THEN the Rent-a-Car System SHALL generate JWT tokens using the same format as traditional login
2. WHEN a social login user's JWT expires THEN the Rent-a-Car System SHALL allow token refresh using the standard refresh token endpoint
3. WHEN a user logs in via social provider THEN the Rent-a-Car System SHALL include the same claims (username, roles) in the JWT as traditional login
4. WHEN a social-only user attempts password login THEN the Rent-a-Car System SHALL return an error indicating social login is required

### Requirement 7: User Profile from Social Provider

**User Story:** As a new user signing up via social login, I want my profile to be pre-populated with my social account information, so that I don't have to enter it manually.

#### Acceptance Criteria

1. WHEN creating a user from social login THEN the Rent-a-Car System SHALL use the provider's email as the user's email
2. WHEN creating a user from social login THEN the Rent-a-Car System SHALL generate a unique username from the provider's name or email prefix
3. WHEN the generated username already exists THEN the Rent-a-Car System SHALL append a numeric suffix to create a unique username
4. WHEN the provider supplies an avatar URL THEN the Rent-a-Car System SHALL store the avatar URL in the user profile
5. WHEN creating a social-only user THEN the Rent-a-Car System SHALL set a null password and mark the account as social-login-only
