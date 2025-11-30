package com.akif.service.oauth2;

import com.akif.dto.oauth2.OAuth2TokenResponse;
import com.akif.dto.oauth2.OAuth2UserInfo;

public interface IOAuth2ProviderService {

    OAuth2TokenResponse exchangeCodeForTokens(String code);

    OAuth2UserInfo getUserInfo(String accessToken);

    String getProviderName();
}
