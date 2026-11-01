package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.emin.roomsplit.expense.SettlementPlanner.Transfer;

class SettlementPlannerTest {

    private static Map<Long, Long> balansi(long... vrijednosti) {
        Map<Long, Long> balances = new LinkedHashMap<>();
        for (int i = 0; i < vrijednosti.length; i++) {
            balances.put((long) (i + 1), vrijednosti[i]);
        }
        return balances;
    }

    // Primijeni uplate na balanse: nakon toga svi moraju biti nula
    private static void primijeniUplate(Map<Long, Long> balances, List<Transfer> transfers) {
        Map<Long, Long> poslije = new HashMap<>(balances);
        for (Transfer t : transfers) {
            poslije.merge(t.fromUserId(), t.feninzi(), Long::sum);   // platilac smanjuje svoj dug
            poslije.merge(t.toUserId(), -t.feninzi(), Long::sum);    // primalac smanjuje potraživanje
        }
        assertThat(poslije.values()).allMatch(vrijednost -> vrijednost == 0);
    }

    // Nezavisan "oracle": isprobava poravnanja rekurzijom i vraća tačan minimalan broj uplata.
    // Potpuno drugačiji pristup od SettlementPlanner-a, pa se poklapanje rezultata računa kao dokaz.
    private static int minimalanBrojUplata(long[] dugovi) {
        return pretrazi(dugovi.clone(), 0);
    }

    private static int pretrazi(long[] dugovi, int pocetak) {
        while (pocetak < dugovi.length && dugovi[pocetak] == 0) {
            pocetak++;
        }
        if (pocetak == dugovi.length) {
            return 0;
        }
        int najbolje = Integer.MAX_VALUE;
        for (int i = pocetak + 1; i < dugovi.length; i++) {
            if (dugovi[i] * dugovi[pocetak] < 0) {
                dugovi[i] += dugovi[pocetak];
                najbolje = Math.min(najbolje, 1 + pretrazi(dugovi, pocetak + 1));
                dugovi[i] -= dugovi[pocetak];
            }
        }
        return najbolje;
    }

    @Test
    void jedanDuznikJedanVjerovnikDajeJednuUplatu() {
        List<Transfer> uplate = SettlementPlanner.plan(balansi(3000, -3000));

        assertThat(uplate).containsExactly(new Transfer(2L, 1L, 3000));
    }

    @Test
    void dvojicaDuzniciPlacajuJednomVjerovniku() {
        // Ana je platila za sve: svako duguje njoj, nema lanca preko trećeg
        List<Transfer> uplate = SettlementPlanner.plan(balansi(6000, -3000, -3000));

        assertThat(uplate).containsExactlyInAnyOrder(new Transfer(2L, 1L, 3000), new Transfer(3L, 1L, 3000));
    }

    @Test
    void lanacDugovaSeSkracuje() {
        // A duguje B 10, B duguje C 10: balansi A -10, B 0, C +10. B ne učestvuje, A plaća direktno C
        List<Transfer> uplate = SettlementPlanner.plan(balansi(-1000, 0, 1000));

        assertThat(uplate).containsExactly(new Transfer(1L, 3L, 1000));
    }

    @Test
    void nepovezaneGrupeDugovaSePoravnavajuSeparatnoSaManjeUplataNegoGreedy() {
        // Dvije nezavisne cjeline: {+5, -5} i {+3, +4, -7}. Optimum je 1 + 2 = 3 uplate.
        // Greedy (najveći dužnik najvećem vjerovniku) bi ovdje napravio 4.
        Map<Long, Long> balances = balansi(500, 300, 400, -500, -700);

        List<Transfer> uplate = SettlementPlanner.plan(balances);

        assertThat(uplate).hasSize(3);
        primijeniUplate(balances, uplate);
    }

    @Test
    void nultiBalansiSeIgnorisuIPraznaGrupaDajePrazanPlan() {
        assertThat(SettlementPlanner.plan(balansi(0, 0, 0))).isEmpty();
        assertThat(SettlementPlanner.plan(Map.of())).isEmpty();
    }

    @Test
    void zbirBalansaKojiNijeNulaJeGreska() {
        assertThatThrownBy(() -> SettlementPlanner.plan(balansi(1000, -900)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void svakaUplataJePozitivnaINikoNePlacaSamSebi() {
        List<Transfer> uplate = SettlementPlanner.plan(balansi(1234, -234, 500, -1500));

        assertThat(uplate).allSatisfy(uplata -> {
            assertThat(uplata.feninzi()).isPositive();
            assertThat(uplata.fromUserId()).isNotEqualTo(uplata.toUserId());
        });
    }

    @Test
    void rezultatJeUvijekIsti() {
        Map<Long, Long> balances = balansi(500, 300, 400, -500, -700);

        assertThat(SettlementPlanner.plan(balances)).isEqualTo(SettlementPlanner.plan(balances));
    }

    @Test
    void brojUplataJeTacnoMinimalanNaStotinamaNasumicnihPrimjera() {
        Random random = new Random(42);   // fiksno sjeme: test je ponovljiv

        for (int pokusaj = 0; pokusaj < 400; pokusaj++) {
            int n = 2 + random.nextInt(7);   // 2..8 učesnika
            long[] vrijednosti = new long[n];
            long zbir = 0;
            for (int i = 0; i < n - 1; i++) {
                // Često nula i mali iznosi da se češće pojave nezavisne podgrupe
                vrijednosti[i] = random.nextInt(4) == 0 ? 0 : (random.nextInt(21) - 10) * 100L;
                zbir += vrijednosti[i];
            }
            vrijednosti[n - 1] = -zbir;
            Map<Long, Long> balances = balansi(vrijednosti);

            List<Transfer> uplate = SettlementPlanner.plan(balances);

            primijeniUplate(balances, uplate);
            assertThat(uplate).as("balansi %s", balances).hasSize(minimalanBrojUplata(vrijednosti));
        }
    }

    @Test
    void velikaGrupaPrelaziNaGreedyIJosUvijekPoravnavaSve() {
        Random random = new Random(7);
        int n = SettlementPlanner.MAX_EXACT + 5;
        long[] vrijednosti = new long[n];
        long zbir = 0;
        for (int i = 0; i < n - 1; i++) {
            vrijednosti[i] = (random.nextInt(2001) - 1000) * 10L + 10;   // različito od nule u većini slučajeva
            zbir += vrijednosti[i];
        }
        vrijednosti[n - 1] = -zbir;
        Map<Long, Long> balances = balansi(vrijednosti);

        List<Transfer> uplate = SettlementPlanner.plan(balances);

        primijeniUplate(balances, uplate);
        assertThat(uplate.size()).isLessThanOrEqualTo(n - 1);
    }

    @Test
    void tacnaGrupaOdDvadesetLjudiSeRacunaBrzo() {
        // Gornja granica tačnog algoritma: 2^20 stanja ne smije trajati dugo
        int n = SettlementPlanner.MAX_EXACT;
        long[] vrijednosti = new long[n];
        for (int i = 0; i < n; i += 2) {
            vrijednosti[i] = (i + 1) * 100L;
            vrijednosti[i + 1] = -(i + 1) * 100L;   // 10 nezavisnih parova
        }
        Map<Long, Long> balances = balansi(vrijednosti);

        long pocetak = System.nanoTime();
        List<Transfer> uplate = SettlementPlanner.plan(balances);
        long trajanjeMs = (System.nanoTime() - pocetak) / 1_000_000;

        assertThat(uplate).hasSize(n / 2);   // jedan par = jedna uplata
        assertThat(trajanjeMs).isLessThan(5000);
    }
}
