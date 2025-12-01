package com.akif.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

@Getter
public class ReconciliationException extends BaseException {

    private final LocalDate reconciliationDate;

    public ReconciliationException(LocalDate reconciliationDate, String message) {
        super("RECONCILIATION_ERROR",
              String.format("Reconciliation failed for date %s: %s", reconciliationDate, message),
              HttpStatus.INTERNAL_SERVER_ERROR);
        this.reconciliationDate = reconciliationDate;
    }

    public ReconciliationException(LocalDate reconciliationDate, String message, Throwable cause) {
        super("RECONCILIATION_ERROR",
              String.format("Reconciliation failed for date %s: %s", reconciliationDate, message),
              HttpStatus.INTERNAL_SERVER_ERROR,
              cause);
        this.reconciliationDate = reconciliationDate;
    }
}
