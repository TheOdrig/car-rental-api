package com.akif.service.oauth2;

import com.akif.dto.oauth2.LinkAccountResponseDto;
import com.akif.dto.response.AuthResponseDto;

public interface IOAuth2AuthService {

    String getAuthorizationUrl(String provider);

    AuthResponseDto processOAuth2Callback(String provider, String code, String state);

    LinkAccountResponseDto linkSocialAccount(String provider, String code, String state, Long userId);
}
