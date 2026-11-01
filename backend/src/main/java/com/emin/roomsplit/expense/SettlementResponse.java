package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.Instant;

public record SettlementResponse(Long id, PersonResponse from, PersonResponse to, BigDecimal amount, Instant settledAt) {

    public static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                PersonResponse.from(settlement.getFromUser()),
                PersonResponse.from(settlement.getToUser()),
                settlement.getAmount(),
                settlement.getSettledAt());
    }
}
