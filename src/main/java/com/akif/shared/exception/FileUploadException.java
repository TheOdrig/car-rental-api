package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends BaseException {

    public static final String STORAGE_FAILURE_MESSAGE = "The file could not be stored. Please try again later.";

    public FileUploadException(String message) {
        super(ErrorCode.FILE_UPLOAD_ERROR.name(), message, HttpStatus.BAD_REQUEST);
    }

    public FileUploadException(String message, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_ERROR.name(), message, HttpStatus.BAD_REQUEST, cause);
    }

    public static FileUploadException storageFailure(Throwable cause) {
        return new FileUploadException(STORAGE_FAILURE_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    private FileUploadException(String message, HttpStatus status, Throwable cause) {
        super(ErrorCode.FILE_UPLOAD_ERROR.name(), message, status, cause);
    }
}
