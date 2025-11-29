package com.akif.dto.oauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkAccountResponseDto {

    private String message;
    private String provider;
    private String providerEmail;
    private String linkedAt;
}
