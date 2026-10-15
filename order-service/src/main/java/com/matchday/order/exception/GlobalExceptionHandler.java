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
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problemDetail.setTitle("Seat not found");

        return problemDetail;
    }

    @ExceptionHandler(SeatNotReservedException.class)
    ProblemDetail handleSeatNotReserved(SeatNotReservedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());

        problemDetail.setTitle("Seat not reserved");

        return problemDetail;
    }

    @ExceptionHandler(TicketServiceUnavailableException.class)
    ProblemDetail handleTicketServiceUnavailable(TicketServiceUnavailableException ex) {
        log.warn("ticket-service unavailable: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Tickets are temporarily unavailable, please try again");

        problemDetail.setTitle("Service unavailable");

        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side");

        problemDetail.setTitle("Internal error");

        return problemDetail;
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    ProblemDetail handleUserServiceUnavailable(UserServiceUnavailableException ex) {
        log.warn("user-service unavailable: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "We could not confirm your account right now, please try again");

        problemDetail.setTitle("Service unavailable");

        return problemDetail;
    }
}
