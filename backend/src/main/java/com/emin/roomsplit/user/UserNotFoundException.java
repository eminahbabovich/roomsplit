package com.emin.roomsplit.user;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Korisnik s id-em " + id + " ne postoji");
    }
}
