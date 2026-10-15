package com.matchday.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(SeatNotFoundException.class)
    ProblemDetail handleSeatNotFound(SeatNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problem.setTitle("Seat not found");

        return problem;
    }

    @ExceptionHandler(SeatNotReservedException.class)
    ProblemDetail handleSeatNotReserved(SeatNotReservedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        problem.setTitle("Seat not reserved");

        return problem;
    }

    // Only reached before any money moved (reading the seat failed), so "try again" is safe advice
    @ExceptionHandler(TicketServiceUnavailableException.class)
    ProblemDetail handleTicketServiceUnavailable(TicketServiceUnavailableException ex) {
        log.warn("ticket-service unavailable: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Tickets are temporarily unavailable, please try again");

        problem.setTitle("Service unavailable");

        return problem;
    }

    // Anything not handled above is our bug: log the details, tell the client nothing internal
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side");

        problem.setTitle("Internal error");

        return problem;
    }
}
