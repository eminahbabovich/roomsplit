package com.emin.roomsplit.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// Jedinstven oblik greške koji API vraća klijentu
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {
}
