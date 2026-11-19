package com.akif.rental.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSummaryResponse(
    Long id,
    String username,
    String email,
    String fullName
) {

    public UserSummaryResponse(Long id, String username, String email) {
        this(id, username, email, null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    public String displayName() {
        if (fullName != null && !fullName.isBlank() && !fullName.contains("null")) {
            return fullName.trim();
        }
        return username;
    }
}
