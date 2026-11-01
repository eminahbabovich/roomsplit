package com.emin.roomsplit.group;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class JoinCodeGenerator {

    // Bez znakova koji se lako brkaju (0/O, 1/I/L), da se kod lakše prepisuje
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static final int LENGTH = 8;

    // SecureRandom, a ne Random: kod je jedina "lozinka" za ulazak u grupu, ne smije se predvidjeti
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
