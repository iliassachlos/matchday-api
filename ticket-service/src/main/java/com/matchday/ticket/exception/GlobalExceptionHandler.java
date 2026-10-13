package com.matchday.ticket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatNotFoundException.class)
    ProblemDetail handleSeatNotFound(SeatNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problem.setTitle("Seat not found");

        return problem;
    }

    @ExceptionHandler(SeatNotAvailableException.class)
    ProblemDetail handleSeatNotAvailable(SeatNotAvailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        problem.setTitle("Seat not available");

        return problem;
    }
}
