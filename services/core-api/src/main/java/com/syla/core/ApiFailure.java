package com.syla.core;

import org.springframework.http.HttpStatus;

/** A safe API failure: its message never includes user input or database details. */
public class ApiFailure extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiFailure(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
