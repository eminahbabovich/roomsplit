package com.emin.roomsplit.expense;

import java.math.BigDecimal;

// Predložena uplata: "from" treba isplatiti "to" iznos amount (KM)
public record TransferResponse(PersonResponse from, PersonResponse to, BigDecimal amount) {
}
