package com.syla.core;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ApiFailure.class)
    ProblemDetail failure(ApiFailure failure) {
        var problem = ProblemDetail.forStatusAndDetail(failure.status(), failure.getMessage());
        problem.setProperty("code", failure.code());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidFields(MethodArgumentNotValidException failure) {
        var problem = badRequest();
        problem.setProperty("fields", failure.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField()).distinct().sorted().toList());
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, org.springframework.web.bind.MissingRequestHeaderException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ProblemDetail invalidInput(Exception ignored) { return badRequest(); }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail databaseFailure(DataAccessException ignored) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Storage is unavailable; retry the same operation.");
        problem.setProperty("code", "storage_unavailable");
        return problem;
    }

    private ProblemDetail badRequest() {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Check the request fields.");
        problem.setProperty("code", "invalid_request");
        return problem;
    }
}
