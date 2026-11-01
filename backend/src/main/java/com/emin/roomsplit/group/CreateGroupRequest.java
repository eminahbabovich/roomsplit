package com.emin.roomsplit.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Naziv grupe je obavezan")
        @Size(max = 100, message = "Naziv može imati najviše 100 znakova")
        String name) {
}
