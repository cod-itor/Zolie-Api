package com.example.zolieapi.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Long retryAfterSeconds;

    public ApiException(ErrorCode code) {
        this(code, code.getDefaultMessage(), null);
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }

    public static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN);
    }

    public static ApiException rule(String message) {
        return new ApiException(ErrorCode.RULE_VIOLATION, message);
    }

    public static ApiException invalid(String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, message);
    }
}
