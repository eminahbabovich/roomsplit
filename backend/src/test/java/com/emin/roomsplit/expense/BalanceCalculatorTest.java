package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BalanceCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal zbir(Map<Long, BigDecimal> balances) {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void platilacJeUPlusuAOstaliSuUMinusu() {
        // Ana (1) je platila 90.00, podijeljeno na troje po 30.00
        Map<Long, BigDecimal> balansi = BalanceCalculator.calculate(List.of(1L, 2L, 3L),
                Map.of(1L, bd("90.00")),
                Map.of(1L, bd("30.00"), 2L, bd("30.00"), 3L, bd("30.00")),
                Map.of(), Map.of());

        assertThat(balansi.get(1L)).isEqualByComparingTo("60.00");
        assertThat(balansi.get(2L)).isEqualByComparingTo("-30.00");
        assertThat(balansi.get(3L)).isEqualByComparingTo("-30.00");
        assertThat(zbir(balansi)).isEqualByComparingTo("0");
    }

    @Test
    void uplataSmanjujeDugPlatiocaIPotrazivanjePrimaoca() {
        // Isti troškovi kao gore, pa je Emir (2) isplatio Ani (1) 30.00
        Map<Long, BigDecimal> balansi = BalanceCalculator.calculate(List.of(1L, 2L, 3L),
                Map.of(1L, bd("90.00")),
                Map.of(1L, bd("30.00"), 2L, bd("30.00"), 3L, bd("30.00")),
                Map.of(2L, bd("30.00")),    // Emir je isplatio
                Map.of(1L, bd("30.00")));   // Ana je primila

        assertThat(balansi.get(1L)).isEqualByComparingTo("30.00");
        assertThat(balansi.get(2L)).isEqualByComparingTo("0.00");
        assertThat(balansi.get(3L)).isEqualByComparingTo("-30.00");
        assertThat(zbir(balansi)).isEqualByComparingTo("0");
    }

    @Test
    void stoKMNaTrojeNeGubiNijedanFening() {
        // 100.00 podijeljeno 33.34 + 33.33 + 33.33; platila je Ana
        Map<Long, BigDecimal> balansi = BalanceCalculator.calculate(List.of(1L, 2L, 3L),
                Map.of(1L, bd("100.00")),
                Map.of(1L, bd("33.34"), 2L, bd("33.33"), 3L, bd("33.33")),
                Map.of(), Map.of());

        assertThat(balansi.get(1L)).isEqualByComparingTo("66.66");
        assertThat(balansi.get(2L)).isEqualByComparingTo("-33.33");
        assertThat(zbir(balansi)).isEqualByComparingTo("0");
    }

    @Test
    void clanBezTroskovaImaBalansNulaSaDvijeDecimale() {
        Map<Long, BigDecimal> balansi = BalanceCalculator.calculate(List.of(1L), Map.of(), Map.of(), Map.of(), Map.of());

        assertThat(balansi.get(1L)).isEqualTo(bd("0.00"));
    }
}
