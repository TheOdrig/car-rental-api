package com.akif.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDto {

    private String errorCode;
    private String message;

    @JsonFormat(pattern = "dd-MM-yyyy'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private Integer status;
    private String path;
    private Map<String, Object> details;
    private Map<String, String> validationErrors;
    private String traceId;
}
