package com.syla.core.platform.internal;

import com.syla.core.platform.ApiProblem;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@RestControllerAdvice
class ProblemHandler {
    @ExceptionHandler(ApiProblem.class)
    ProblemDetail domain(ApiProblem error, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        var problem = ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatusCode.valueOf(error.status()), error.getMessage());
        problem.setProperty("errors", error.errors());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException exception, HttpServletResponse response) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return domain(new ApiProblem(400, "Check the highlighted fields.", errors), response);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail malformed(Exception exception, HttpServletResponse response) {
        return domain(new ApiProblem(400, "The request contains an invalid value or format."), response);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail conflict(Exception exception, HttpServletResponse response) {
        return domain(new ApiProblem(409, "This activity changed elsewhere. Reload before editing again."), response);
    }
}
