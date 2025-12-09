package com.akif.auth.internal.oauth2;

import com.akif.auth.internal.oauth2.dto.request.OAuth2UserInfo;
import com.akif.auth.internal.oauth2.dto.response.OAuth2TokenResponse;

public interface IOAuth2ProviderService {

    OAuth2TokenResponse exchangeCodeForTokens(String code);

    OAuth2UserInfo getUserInfo(String accessToken);

    String getProviderName();
}
