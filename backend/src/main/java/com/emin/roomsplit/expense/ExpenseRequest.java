package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Isti zahtjev služi za dodavanje i za izmjenu (PUT zamjenjuje cijeli trošak)
public record ExpenseRequest(
        @NotBlank(message = "Opis je obavezan")
        @Size(max = 255, message = "Opis može imati najviše 255 znakova")
        String description,

        @NotNull(message = "Iznos je obavezan")
        @DecimalMin(value = "0.01", message = "Iznos mora biti najmanje 0.01 KM")
        @Digits(integer = 10, fraction = 2, message = "Iznos može imati najviše 10 cifara i 2 decimale")
        BigDecimal amount,

        @NotNull(message = "Platilac je obavezan")
        Long paidBy,

        // Ako se ne pošalje, trošak dobija kategoriju OTHER
        ExpenseCategory category,

        @NotNull(message = "Datum je obavezan")
        LocalDate expenseDate,

        @NotNull(message = "Tip podjele je obavezan (EQUAL ili EXACT)")
        SplitType splitType,

        @NotEmpty(message = "Potreban je bar jedan učesnik")
        List<@Valid @NotNull ParticipantRequest> participants) {
}
