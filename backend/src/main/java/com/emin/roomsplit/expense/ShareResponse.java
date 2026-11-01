package com.emin.roomsplit.expense;

import java.math.BigDecimal;

public record ShareResponse(Long userId, String displayName, BigDecimal amount) {

    public static ShareResponse from(ExpenseShare share) {
        return new ShareResponse(share.getUser().getId(), share.getUser().getDisplayName(), share.getShareAmount());
    }
}
