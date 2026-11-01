package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(
        Long id,
        Long groupId,
        String description,
        BigDecimal amount,
        PersonResponse paidBy,
        PersonResponse createdBy,
        ExpenseCategory category,
        LocalDate expenseDate,
        Instant createdAt,
        List<ShareResponse> shares) {

    // Dijelovi se šalju posebno, jer ih za listu učitavamo jednim upitom za sve troškove odjednom
    public static ExpenseResponse from(Expense expense, List<ExpenseShare> shares) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getDescription(),
                expense.getAmount(),
                PersonResponse.from(expense.getPaidBy()),
                PersonResponse.from(expense.getCreatedBy()),
                expense.getCategory(),
                expense.getExpenseDate(),
                expense.getCreatedAt(),
                shares.stream().map(ShareResponse::from).toList());
    }
}
