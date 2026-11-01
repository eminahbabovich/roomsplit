package com.emin.roomsplit.expense;

import java.math.BigDecimal;

// balance > 0: drugi duguju ovom članu; balance < 0: on duguje drugima (KM)
public record BalanceResponse(Long userId, String displayName, BigDecimal balance) {
}
