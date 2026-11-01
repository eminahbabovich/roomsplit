package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

// Balans korisnika u grupi (KM). Pozitivan: drugi duguju njemu. Negativan: on duguje drugima.
//
//   balans = platio kroz troškove - njegov dio troškova + isplatio kroz uplate - primio kroz uplate
//
// Uplata A -> B smanjuje A-ov dug (njegov balans raste), a B-ovo potraživanje (njegov balans pada).
// Zbir balansa svih članova je uvijek tačno nula.
public final class BalanceCalculator {

    private BalanceCalculator() {
    }

    public static Map<Long, BigDecimal> calculate(Collection<Long> memberIds,
            Map<Long, BigDecimal> paid, Map<Long, BigDecimal> owed,
            Map<Long, BigDecimal> settledOut, Map<Long, BigDecimal> settledIn) {

        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (Long id : memberIds) {
            BigDecimal balance = paid.getOrDefault(id, BigDecimal.ZERO)
                    .subtract(owed.getOrDefault(id, BigDecimal.ZERO))
                    .add(settledOut.getOrDefault(id, BigDecimal.ZERO))
                    .subtract(settledIn.getOrDefault(id, BigDecimal.ZERO));
            balances.put(id, balance.setScale(2));
        }
        return balances;
    }
}
