package com.emin.roomsplit.auth;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Korisnik s emailom " + email + " već postoji");
    }
}
