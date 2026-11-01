package com.emin.roomsplit.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.emin.roomsplit.auth.EmailAlreadyExistsException;
import com.emin.roomsplit.auth.InvalidCredentialsException;
import com.emin.roomsplit.user.UserNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Neispravni podaci", fieldErrors);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleInvalidCredentials(InvalidCredentialsException ex) {
        return new ApiError(Instant.now(), HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), null);
    }

    // Token je važeći, ali korisnik je u međuvremenu obrisan
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleUserNotFound(UserNotFoundException ex) {
        return new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailExists(EmailAlreadyExistsException ex) {
        return new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), ex.getMessage(), null);
    }
}
