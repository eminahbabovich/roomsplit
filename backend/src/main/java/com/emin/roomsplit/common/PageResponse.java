package com.emin.roomsplit.common;

import java.util.List;

import org.springframework.data.domain.Page;

// Vlastiti oblik stranice: Springov Page nema stabilan JSON oblik, a ovako ga mi određujemo
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> items) {
        return new PageResponse<>(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
