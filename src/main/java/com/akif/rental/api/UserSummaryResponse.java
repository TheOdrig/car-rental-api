package com.akif.rental.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSummaryResponse(
    Long id,
    String username,
    String email
) {}
