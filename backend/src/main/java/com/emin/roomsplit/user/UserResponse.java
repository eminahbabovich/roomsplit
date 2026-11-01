package com.emin.roomsplit.user;

import java.time.Instant;

// Ono što API vraća o korisniku. Namjerno nema passwordHash.
public record UserResponse(Long id, String email, String displayName, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
