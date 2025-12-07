package com.akif.exception;

import org.springframework.http.HttpStatus;

public class DamageDisputeException extends BaseException {

    public DamageDisputeException(String message) {
        super("DAMAGE_DISPUTE_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public DamageDisputeException(String message, HttpStatus httpStatus) {
        super("DAMAGE_DISPUTE_ERROR", message, httpStatus);
    }

    public static DamageDisputeException invalidStatus(String currentStatus) {
        return new DamageDisputeException(
                "Cannot create dispute in current status: " + currentStatus
        );
    }

    public static DamageDisputeException cannotResolve(String currentStatus) {
        return new DamageDisputeException(
                "Cannot resolve dispute in current status: " + currentStatus
        );
    }

    public static DamageDisputeException notOwner() {
        return new DamageDisputeException(
                "You can only dispute your own rental damages",
                HttpStatus.FORBIDDEN
        );
    }
}
