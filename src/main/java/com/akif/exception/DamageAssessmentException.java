package com.akif.exception;

import org.springframework.http.HttpStatus;

public class DamageAssessmentException extends BaseException {

    public DamageAssessmentException(String message) {
        super("DAMAGE_ASSESSMENT_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public DamageAssessmentException(String message, HttpStatus httpStatus) {
        super("DAMAGE_ASSESSMENT_ERROR", message, httpStatus);
    }

    public static DamageAssessmentException invalidStatus(String currentStatus) {
        return new DamageAssessmentException(
                "Cannot assess damage in current status: " + currentStatus
        );
    }

    public static DamageAssessmentException alreadyCharged() {
        return new DamageAssessmentException(
                "Cannot update assessment after charge has been created"
        );
    }
}
