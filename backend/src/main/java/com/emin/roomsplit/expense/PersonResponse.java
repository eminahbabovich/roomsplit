package com.emin.roomsplit.expense;

import com.emin.roomsplit.user.User;

public record PersonResponse(Long userId, String displayName) {

    public static PersonResponse from(User user) {
        return new PersonResponse(user.getId(), user.getDisplayName());
    }
}
