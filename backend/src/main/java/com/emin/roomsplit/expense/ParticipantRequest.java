package com.emin.roomsplit.expense;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

// amount je obavezan samo kod podjele EXACT (to provjerava servis); kod EQUAL se ignoriše
public record ParticipantRequest(
        @NotNull(message = "Učesnik mora imati userId") Long userId,
        BigDecimal amount) {
}
