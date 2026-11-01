package com.emin.roomsplit.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email je obavezan") String email,
        @NotBlank(message = "Lozinka je obavezna") String password) {
}
