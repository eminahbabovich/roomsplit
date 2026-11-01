package com.emin.roomsplit.common;

// Zahtjev je sintaksno ispravan, ali nema smisla (npr. početni datum poslije krajnjeg)
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
