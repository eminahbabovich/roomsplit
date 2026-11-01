package com.emin.roomsplit.group;

import jakarta.validation.constraints.NotBlank;

public record JoinGroupRequest(@NotBlank(message = "Kod za pridruživanje je obavezan") String joinCode) {
}
