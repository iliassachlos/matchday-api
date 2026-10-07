package com.matchday.user.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(EmailAlreadyUsedException.class)
  ProblemDetail handleEmailAlreadyUsed(EmailAlreadyUsedException exception) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());

    problemDetail.setTitle("Email already registered");

    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");

    problemDetail.setTitle("Invalid request");
    problemDetail.setType(URI.create("https://matchday.dev/errors/validation"));
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
    problemDetail.setProperty("errors", fieldErrors);

    return problemDetail;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource already exists");

    problemDetail.setTitle("Conflict");

    return problemDetail;
  }
}
