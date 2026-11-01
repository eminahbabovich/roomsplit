package com.emin.roomsplit.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email je obavezan")
        @Email(message = "Email nije ispravnog formata")
        @Size(max = 255, message = "Email može imati najviše 255 znakova")
        String email,

        // BCrypt gleda samo prvih 72 bajta lozinke, zato je to gornja granica
        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 8, max = 72, message = "Lozinka mora imati između 8 i 72 znaka")
        String password,

        @NotBlank(message = "Ime za prikaz je obavezno")
        @Size(max = 100, message = "Ime može imati najviše 100 znakova")
        String displayName) {
}
