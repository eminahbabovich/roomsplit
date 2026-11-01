package com.emin.roomsplit.expense;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException() {
        super("Trošak ne postoji");
    }
}
