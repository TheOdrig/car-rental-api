package com.akif.dto.email;

import com.akif.shared.enums.EmailType;

public record EmailMessage(
    String to,
    String subject,
    String body,
    EmailType type,
    Long referenceId
) {
}
