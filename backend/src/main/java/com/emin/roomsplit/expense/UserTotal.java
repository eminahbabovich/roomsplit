package com.emin.roomsplit.expense;

import java.math.BigDecimal;

// Rezultat zbirnog upita: zbir iznosa po korisniku
public record UserTotal(Long userId, BigDecimal total) {
}
