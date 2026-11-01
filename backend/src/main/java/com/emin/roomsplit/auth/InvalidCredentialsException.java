package com.emin.roomsplit.auth;

public class InvalidCredentialsException extends RuntimeException {

    // Poruka je namjerno ista za pogrešan email i pogrešnu lozinku
    public InvalidCredentialsException() {
        super("Pogrešan email ili lozinka");
    }
}
