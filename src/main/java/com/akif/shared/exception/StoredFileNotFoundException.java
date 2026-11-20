package com.akif.shared.exception;

import org.springframework.http.HttpStatus;

public class StoredFileNotFoundException extends BaseException {

    public StoredFileNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND.name(), "The requested file was not found", HttpStatus.NOT_FOUND);
    }
}
