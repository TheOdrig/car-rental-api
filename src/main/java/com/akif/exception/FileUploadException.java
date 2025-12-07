package com.akif.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends BaseException {

    public FileUploadException(String message) {
        super("FILE_UPLOAD_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public FileUploadException(String message, Throwable cause) {
        super("FILE_UPLOAD_ERROR", message, HttpStatus.BAD_REQUEST, cause);
    }
}
