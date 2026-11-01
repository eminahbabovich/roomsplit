package com.emin.roomsplit.expense;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Predlaže uplate kojima se svi dugovi poravnaju uz NAJMANJI mogući broj uplata.
//
// Ulaz: balans svakog korisnika u feninzima (pozitivan = drugi duguju njemu, negativan = on duguje).
// Zbir balansa mora biti nula.
//
// Ideja: ako se korisnici mogu podijeliti u k nezavisnih grupa čiji balansi zbrojeni daju nulu, svaka grupa
// od m ljudi se poravna s m - 1 uplata, pa je ukupno n - k uplata. Minimum se dobije kad je k najveći mogući.
// Traženje najvećeg k je dinamičko programiranje nad podskupovima (2^n), pa to radimo samo do MAX_EXACT ljudi;
// za veće grupe koristimo greedy koji je i dalje ispravan (svi se poravnaju, najviše n - 1 uplata), ali ne
// mora biti najmanji mogući.
public final class SettlementPlanner {

    public record Transfer(long fromUserId, long toUserId, long feninzi) {
    }

    // 2^20 stanja je oko milion, što je brzo; iznad toga prelazimo na greedy
    static final int MAX_EXACT = 20;

    private SettlementPlanner() {
    }

    public static List<Transfer> plan(Map<Long, Long> balances) {
        long total = balances.values().stream().mapToLong(Long::longValue).sum();
        if (total != 0) {
            throw new IllegalArgumentException("Zbir balansa mora biti nula, a iznosi " + total);
        }

        // Samo ljudi s dugom ili potraživanjem; poredani po id-u da rezultat bude uvijek isti
        List<Map.Entry<Long, Long>> active = balances.entrySet().stream()
                .filter(entry -> entry.getValue() != 0)
                .sorted(Map.Entry.comparingByKey())
                .toList();
        int n = active.size();
        if (n == 0) {
            return List.of();
        }

        long[] ids = new long[n];
        long[] amounts = new long[n];
        for (int i = 0; i < n; i++) {
            ids[i] = active.get(i).getKey();
            amounts[i] = active.get(i).getValue();
        }

        List<List<Integer>> blocks = n <= MAX_EXACT ? zeroSumBlocks(amounts) : List.of(allIndices(n));

        List<Transfer> transfers = new ArrayList<>();
        for (List<Integer> block : blocks) {
            transfers.addAll(settleBlock(ids, amounts, block));
        }
        return transfers;
    }

    private static List<Integer> allIndices(int n) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            all.add(i);
        }
        return all;
    }

    // Dijeli ljude na najveći mogući broj grupa čiji balansi zbrojeni daju nulu.
    // dp[mask] = najviše puta koliko se u nekom redoslijedu elemenata iz mask-a "pogodi" zbir nula.
    // Elementi se redaju jedan po jedan, a svaki put kad zbir do tada poredanih bude nula, zatvara se jedna grupa.
    private static List<List<Integer>> zeroSumBlocks(long[] amounts) {
        int n = amounts.length;
        int full = (1 << n) - 1;

        long[] sum = new long[full + 1];
        int[] dp = new int[full + 1];
        int[] last = new int[full + 1];

        for (int mask = 1; mask <= full; mask++) {
            int lowest = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + amounts[lowest];

            int best = -1;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    int candidate = dp[mask ^ (1 << i)];
                    if (candidate > best) {
                        best = candidate;
                        last[mask] = i;
                    }
                }
            }
            dp[mask] = best + (sum[mask] == 0 ? 1 : 0);
        }

        // Rekonstrukcija: redoslijed ukljanjanja, pa se obrne da dobijemo redoslijed dodavanja
        List<Integer> order = new ArrayList<>();
        for (int mask = full; mask != 0; mask ^= 1 << last[mask]) {
            order.add(last[mask]);
        }
        java.util.Collections.reverse(order);

        List<List<Integer>> blocks = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        long running = 0;
        for (int index : order) {
            current.add(index);
            running += amounts[index];
            if (running == 0) {
                blocks.add(current);
                current = new ArrayList<>();
            }
        }
        return blocks;
    }

    // Unutar jedne grupe: najveći dužnik plaća najvećem vjerovniku, pa se ponavlja. Za grupu od m ljudi
    // koja se ne može dalje podijeliti to je tačno m - 1 uplata.
    private static List<Transfer> settleBlock(long[] ids, long[] amounts, List<Integer> block) {
        List<Integer> debtors = block.stream().filter(i -> amounts[i] < 0)
                .sorted(Comparator.<Integer>comparingLong(i -> amounts[i]).thenComparingLong(i -> ids[i]))
                .toList();
        List<Integer> creditors = block.stream().filter(i -> amounts[i] > 0)
                .sorted(Comparator.<Integer>comparingLong(i -> -amounts[i]).thenComparingLong(i -> ids[i]))
                .toList();

        long[] debt = debtors.stream().mapToLong(i -> -amounts[i]).toArray();
        long[] credit = creditors.stream().mapToLong(i -> amounts[i]).toArray();

        List<Transfer> transfers = new ArrayList<>();
        int d = 0;
        int c = 0;
        while (d < debt.length && c < credit.length) {
            long amount = Math.min(debt[d], credit[c]);
            transfers.add(new Transfer(ids[debtors.get(d)], ids[creditors.get(c)], amount));
            debt[d] -= amount;
            credit[c] -= amount;
            if (debt[d] == 0) {
                d++;
            }
            if (credit[c] == 0) {
                c++;
            }
        }
        return transfers;
    }
}
