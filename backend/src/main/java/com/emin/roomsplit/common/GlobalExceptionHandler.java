package com.emin.roomsplit.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.emin.roomsplit.auth.EmailAlreadyExistsException;
import com.emin.roomsplit.auth.InvalidCredentialsException;
import com.emin.roomsplit.expense.ExpenseNotFoundException;
import com.emin.roomsplit.expense.InvalidSplitException;
import com.emin.roomsplit.expense.NotExpenseAuthorException;
import com.emin.roomsplit.group.AlreadyGroupMemberException;
import com.emin.roomsplit.group.GroupNotFoundException;
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

    @ExceptionHandler({ InvalidSplitException.class, InvalidRequestException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleInvalidInput(RuntimeException ex) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null);
    }

    // Loš JSON, nepoznata vrijednost enuma (npr. splitType) ili loš format datuma u tijelu zahtjeva
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException ex) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Zahtjev nije ispravan: provjeri JSON, datum (yyyy-MM-dd) i vrijednosti kao što su splitType i category", null);
    }

    // Loš parametar u URL-u (npr. from=sutra ili category=NESTO)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadParameter(MethodArgumentTypeMismatchException ex) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Neispravna vrijednost parametra '" + ex.getName() + "'", null);
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleExpenseNotFound(ExpenseNotFoundException ex) {
        return new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
    }

    // 403, a ne 404: korisnik je član grupe i trošak vidi, ali nema pravo da ga mijenja
    @ExceptionHandler(NotExpenseAuthorException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleNotAuthor(NotExpenseAuthorException ex) {
        return new ApiError(Instant.now(), HttpStatus.FORBIDDEN.value(), ex.getMessage(), null);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleGroupNotFound(GroupNotFoundException ex) {
        return new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), ex.getMessage(), null);
    }

    @ExceptionHandler(AlreadyGroupMemberException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleAlreadyMember(AlreadyGroupMemberException ex) {
        return new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), ex.getMessage(), null);
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
