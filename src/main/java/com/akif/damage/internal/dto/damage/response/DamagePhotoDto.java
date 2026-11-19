package com.akif.damage.internal.dto.damage.response;

import com.fasterxml.jackson.annotation.JsonInclude;


import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamagePhotoDto(

    Long id,
    String fileName,
    String secureUrl,
    Long fileSize,
    Instant uploadedAt
) {}
