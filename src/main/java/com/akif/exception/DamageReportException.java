package com.akif.exception;

import org.springframework.http.HttpStatus;

public class DamageReportException extends BaseException {

    public DamageReportException(String message) {
        super("DAMAGE_REPORT_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public DamageReportException(String message, HttpStatus httpStatus) {
        super("DAMAGE_REPORT_ERROR", message, httpStatus);
    }

    public static DamageReportException notFound(Long damageId) {
        return new DamageReportException(
                "Damage report not found with id: " + damageId,
                HttpStatus.NOT_FOUND
        );
    }

    public static DamageReportException photoNotFound(Long photoId) {
        return new DamageReportException(
                "Damage photo not found with id: " + photoId,
                HttpStatus.NOT_FOUND
        );
    }
}
