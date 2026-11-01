package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExpenseSplitterTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static BigDecimal zbir(Map<Long, BigDecimal> shares) {
        return shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- jednaka podjela ---

    @Test
    void stoNaTriDajeProstorOstatkuPrvomUListi() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("100.00"), List.of(1L, 2L, 3L));

        assertThat(shares).containsEntry(1L, bd("33.34"))
                .containsEntry(2L, bd("33.33"))
                .containsEntry(3L, bd("33.33"));
        assertThat(zbir(shares)).isEqualByComparingTo("100.00");
    }

    @Test
    void djeljivIznosSeDijeliTacnoPoJednako() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("10.00"), List.of(1L, 2L, 3L, 4L));

        assertThat(shares.values()).allSatisfy(dio -> assertThat(dio).isEqualByComparingTo("2.50"));
    }

    @Test
    void jedanFeningNaTriOsobeDajeFeningSamoPrvoj() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("0.01"), List.of(1L, 2L, 3L));

        assertThat(shares).containsEntry(1L, bd("0.01"))
                .containsEntry(2L, bd("0.00"))
                .containsEntry(3L, bd("0.00"));
        assertThat(zbir(shares)).isEqualByComparingTo("0.01");
    }

    @Test
    void jedanUcesnikDobijaCijeliIznos() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("49.99"), List.of(7L));

        assertThat(shares).containsOnlyKeys(7L).containsEntry(7L, bd("49.99"));
    }

    @Test
    void iznosBezDecimalaIPrihvacenJeIDajeDvijeDecimale() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("30"), List.of(1L, 2L));

        assertThat(shares.get(1L)).isEqualTo(bd("15.00"));
    }

    @Test
    void zbirDijelovaJeUvijekTacnoJednakUkupnom() {
        // Za mnogo iznosa i broja učesnika: nijedan fening se ne smije izgubiti ni dobiti,
        // a dijelovi se međusobno razlikuju najviše za 1 fening
        for (long feninzi = 1; feninzi <= 5000; feninzi += 7) {
            BigDecimal total = BigDecimal.valueOf(feninzi, 2);
            for (int n = 1; n <= 12; n++) {
                List<Long> ids = new ArrayList<>();
                for (long i = 1; i <= n; i++) {
                    ids.add(i);
                }

                Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(total, ids);

                assertThat(zbir(shares)).as("iznos %s na %s osoba", total, n).isEqualByComparingTo(total);
                BigDecimal najveci = shares.values().stream().max(BigDecimal::compareTo).orElseThrow();
                BigDecimal najmanji = shares.values().stream().min(BigDecimal::compareTo).orElseThrow();
                assertThat(najveci.subtract(najmanji)).isLessThanOrEqualTo(bd("0.01"));
            }
        }
    }

    @Test
    void odbijaIznosNulaINegativan() {
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("0"), List.of(1L)))
                .isInstanceOf(InvalidSplitException.class);
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("-5.00"), List.of(1L)))
                .isInstanceOf(InvalidSplitException.class);
    }

    @Test
    void odbijaViseOdDvijeDecimaleUmjestoDaZaokruzujeTiho() {
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("10.005"), List.of(1L, 2L)))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("2 decimale");
    }

    @Test
    void dozvoljavaZavrsneNuleIzaDrugeDecimale() {
        Map<Long, BigDecimal> shares = ExpenseSplitter.splitEqually(bd("10.0000"), List.of(1L, 2L));

        assertThat(zbir(shares)).isEqualByComparingTo("10.00");
    }

    @Test
    void odbijaIznosVeciOdOnogaStoStajeUBazu() {
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("10000000000.00"), List.of(1L)))
                .isInstanceOf(InvalidSplitException.class);
    }

    @Test
    void odbijaPraznuListuIDuplikate() {
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("10.00"), List.of()))
                .isInstanceOf(InvalidSplitException.class);
        assertThatThrownBy(() -> ExpenseSplitter.splitEqually(bd("10.00"), List.of(1L, 1L)))
                .isInstanceOf(InvalidSplitException.class);
    }

    // --- tačni iznosi ---

    @Test
    void tacniIznosiKojiPogadjajuZbirSuPrihvaceni() {
        Map<Long, BigDecimal> unos = new LinkedHashMap<>();
        unos.put(1L, bd("60.50"));
        unos.put(2L, bd("39.5"));

        Map<Long, BigDecimal> shares = ExpenseSplitter.validateExact(bd("100.00"), unos);

        assertThat(shares).containsEntry(1L, bd("60.50")).containsEntry(2L, bd("39.50"));
    }

    @Test
    void tacniIznosiKojiNePogadjajuZbirSuOdbijeni() {
        Map<Long, BigDecimal> unos = Map.of(1L, bd("60.00"), 2L, bd("39.99"));

        assertThatThrownBy(() -> ExpenseSplitter.validateExact(bd("100.00"), unos))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("Zbir");
    }

    @Test
    void tacniIznosiSaNegativnimIliNultimDijelomSuOdbijeni() {
        assertThatThrownBy(() -> ExpenseSplitter.validateExact(bd("100.00"), Map.of(1L, bd("110.00"), 2L, bd("-10.00"))))
                .isInstanceOf(InvalidSplitException.class);
        assertThatThrownBy(() -> ExpenseSplitter.validateExact(bd("100.00"), Map.of(1L, bd("100.00"), 2L, bd("0.00"))))
                .isInstanceOf(InvalidSplitException.class);
    }

    @Test
    void tacniIznosiSaTriDecimaleSuOdbijeni() {
        assertThatThrownBy(() -> ExpenseSplitter.validateExact(bd("1.00"), Map.of(1L, bd("0.333"), 2L, bd("0.667"))))
                .isInstanceOf(InvalidSplitException.class);
    }

    @Test
    void praznaMapaTacnihIznosaJeOdbijena() {
        assertThatThrownBy(() -> ExpenseSplitter.validateExact(bd("10.00"), Map.of()))
                .isInstanceOf(InvalidSplitException.class);
    }
}
