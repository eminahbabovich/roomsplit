package com.emin.roomsplit.expense;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

// Uplatu uvijek bilježi onaj ko plaća (prijavljeni korisnik), pa "od koga" nije u zahtjevu
public record SettlementRequest(
        @NotNull(message = "Primalac je obavezan") Long toUser,

        @NotNull(message = "Iznos je obavezan")
        @DecimalMin(value = "0.01", message = "Iznos mora biti najmanje 0.01 KM")
        @Digits(integer = 10, fraction = 2, message = "Iznos može imati najviše 10 cifara i 2 decimale")
        BigDecimal amount) {
}
