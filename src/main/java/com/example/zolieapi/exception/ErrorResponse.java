package com.example.zolieapi.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.getStatus().value(), code.name(), message);
    }

    public static ResponseEntity<ErrorResponse> entity(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(of(code, message));
    }

    public static ResponseEntity<ErrorResponse> entity(ApiException e) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.getCode().getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (e.getRetryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()));
        }
        return builder.body(of(e.getCode(), e.getMessage()));
    }

    public String toJson() {
        return "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\"}".formatted(status, code, message.replace("\"", "'"));
    }
}
