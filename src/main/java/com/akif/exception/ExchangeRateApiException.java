package com.akif.exception;

import lombok.Getter;

@Getter
public class ExchangeRateApiException extends RuntimeException {

    private final int statusCode;
    private final String apiMessage;

    public ExchangeRateApiException(String message) {
        super(message);
        this.statusCode = 0;
        this.apiMessage = message;
    }

    public ExchangeRateApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
        this.apiMessage = message;
    }

    public ExchangeRateApiException(int statusCode, String apiMessage) {
        super("Exchange Rate API error: " + statusCode + " - " + apiMessage);
        this.statusCode = statusCode;
        this.apiMessage = apiMessage;
    }

}
