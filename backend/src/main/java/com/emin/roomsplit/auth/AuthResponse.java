package com.emin.roomsplit.auth;

import com.emin.roomsplit.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
}
