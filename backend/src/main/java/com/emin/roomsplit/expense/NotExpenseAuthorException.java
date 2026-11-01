package com.emin.roomsplit.expense;

public class NotExpenseAuthorException extends RuntimeException {

    public NotExpenseAuthorException() {
        super("Samo autor troška može da ga mijenja ili briše");
    }
}
