package com.example.zolieapi.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "The request is not valid."),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Sign in to continue."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The sign-in token is not valid or has expired."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to do this."),
    PROFILE_REQUIRED(HttpStatus.FORBIDDEN, "Finish sign-up with POST /me/sync first."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "This account is suspended."),
    UNDER_18(HttpStatus.FORBIDDEN, "Dating is available from age 18."),
    DATING_PROFILE_REQUIRED(HttpStatus.FORBIDDEN, "Create a dating profile first."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "This method is not supported here."),
    CONFLICT(HttpStatus.CONFLICT, "This conflicts with existing data."),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "This username is already taken."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "This email already has an account."),
    INTRO_ALREADY_SENT(HttpStatus.CONFLICT, "You can only send one intro to this person."),
    ALREADY_ACTED(HttpStatus.CONFLICT, "You already liked or passed this person."),
    ALREADY_VOTED(HttpStatus.CONFLICT, "You already voted in this poll."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "You already reported this."),
    ALREADY_ANSWERED(HttpStatus.CONFLICT, "You already answered this sticker."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "The file is too big."),
    RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "The request breaks a rule."),
    UNDER_13(HttpStatus.UNPROCESSABLE_ENTITY, "You must be at least 13 years old."),
    POLL_ENDED(HttpStatus.UNPROCESSABLE_ENTITY, "This poll has ended."),
    DAILY_PICKS_USED(HttpStatus.TOO_MANY_REQUESTS, "You used all of today's picks. New picks arrive tomorrow."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Try again later."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.");

    private final HttpStatus status;
    private final String defaultMessage;
}
