package com.akif.damage.internal.exception;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.exception.BaseException;
import org.springframework.http.HttpStatus;

public class DamageAssessmentException extends BaseException {

    public DamageAssessmentException(String message) {
        super(ErrorCode.DAMAGE_ASSESSMENT_ERROR.name(), message, HttpStatus.BAD_REQUEST);
    }

    public DamageAssessmentException(String message, HttpStatus httpStatus) {
        super(ErrorCode.DAMAGE_ASSESSMENT_ERROR.name(), message, httpStatus);
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
