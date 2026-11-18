package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends BaseException {

    public FileUploadException(String message) {
        super(ErrorCode.FILE_UPLOAD_ERROR.name(), message, HttpStatus.BAD_REQUEST);
    }

    public FileUploadException(String message, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_ERROR.name(), message, HttpStatus.BAD_REQUEST, cause);
    }
}
