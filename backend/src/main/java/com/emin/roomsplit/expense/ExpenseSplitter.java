package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Dijeli novac bez ikakvog gubitka feninga. Sve računamo u feninzima (cijeli brojevi), pa nema
// zaokruživanja: zbir dijelova je uvijek tačno jednak ukupnom iznosu.
public final class ExpenseSplitter {

    private static final int SCALE = 2;

    // Najveći iznos koji staje u kolonu NUMERIC(12,2)
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private ExpenseSplitter() {
    }

    // Jednaka podjela. Ostatak (feninge koji se ne mogu podijeliti) dobijaju prvi u listi, po 1 fening.
    // Primjer: 100.00 na 3 osobe -> 33.34, 33.33, 33.33
    public static Map<Long, BigDecimal> splitEqually(BigDecimal total, List<Long> userIds) {
        long totalFeninzi = toFeninzi(total);
        requireParticipants(userIds);

        long count = userIds.size();
        long base = totalFeninzi / count;
        long remainder = totalFeninzi % count;

        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            long feninzi = base + (i < remainder ? 1 : 0);
            shares.put(userIds.get(i), fromFeninzi(feninzi));
        }
        return shares;
    }

    // Tačni iznosi koje je korisnik sam unio: provjeravamo da su ispravni i da zbir pogađa ukupan iznos.
    public static Map<Long, BigDecimal> validateExact(BigDecimal total, Map<Long, BigDecimal> amounts) {
        long totalFeninzi = toFeninzi(total);
        if (amounts == null || amounts.isEmpty()) {
            throw new InvalidSplitException("Podjela mora imati bar jednog učesnika");
        }

        long sumFeninzi = 0;
        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : amounts.entrySet()) {
            long feninzi = toFeninzi(entry.getValue());
            sumFeninzi += feninzi;
            shares.put(entry.getKey(), fromFeninzi(feninzi));
        }

        if (sumFeninzi != totalFeninzi) {
            throw new InvalidSplitException("Zbir dijelova (" + fromFeninzi(sumFeninzi)
                    + ") mora biti jednak ukupnom iznosu (" + fromFeninzi(totalFeninzi) + ")");
        }
        return shares;
    }

    private static void requireParticipants(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new InvalidSplitException("Podjela mora imati bar jednog učesnika");
        }
        Set<Long> unique = new HashSet<>(userIds);
        if (unique.size() != userIds.size()) {
            throw new InvalidSplitException("Isti učesnik se ne smije navesti dvaput");
        }
    }

    // Iznos mora biti pozitivan i imati najviše 2 decimale; ništa se ne zaokružuje tiho.
    private static long toFeninzi(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidSplitException("Iznos mora biti veći od nule");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidSplitException("Iznos može imati najviše 2 decimale");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidSplitException("Iznos ne može biti veći od " + MAX_AMOUNT);
        }
        return amount.movePointRight(SCALE).longValueExact();
    }

    private static BigDecimal fromFeninzi(long feninzi) {
        return BigDecimal.valueOf(feninzi, SCALE);
    }
}
