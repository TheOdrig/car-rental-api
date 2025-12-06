package com.akif.dto.damage.response;

import com.fasterxml.jackson.annotation.JsonInclude;


import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DamagePhotoDto(

    Long id,
    String fileName,
    String secureUrl,
    Long fileSize,
    LocalDateTime uploadedAt
) {}
