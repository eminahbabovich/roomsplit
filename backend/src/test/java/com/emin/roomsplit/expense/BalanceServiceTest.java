package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.emin.roomsplit.group.GroupAccess;
import com.emin.roomsplit.group.GroupNotFoundException;
import com.emin.roomsplit.user.User;

// Čisti unit test: repozitorije i provjeru članstva glumi Mockito, baza nije potrebna
@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private GroupAccess groupAccess;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseShareRepository expenseShareRepository;
    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private BalanceService balanceService;

    private Map<Long, User> clanovi;

    private User korisnik(long id, String ime) {
        User user = new User(ime.toLowerCase() + "@example.com", "HASH", ime);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @BeforeEach
    void postavi() {
        clanovi = new LinkedHashMap<>();
        clanovi.put(1L, korisnik(1, "Ana"));
        clanovi.put(2L, korisnik(2, "Emir"));
        clanovi.put(3L, korisnik(3, "Sara"));
    }

    // Ana je platila 100.00, trošak je podijeljen na troje (33.34 + 33.33 + 33.33)
    private void trosakOdStoKM() {
        when(expenseRepository.sumPaidByUser(10L)).thenReturn(List.of(new UserTotal(1L, new BigDecimal("100.00"))));
        when(expenseShareRepository.sumOwedByUser(10L)).thenReturn(List.of(
                new UserTotal(1L, new BigDecimal("33.34")),
                new UserTotal(2L, new BigDecimal("33.33")),
                new UserTotal(3L, new BigDecimal("33.33"))));
        when(settlementRepository.sumPaidOutByUser(10L)).thenReturn(List.of());
        when(settlementRepository.sumReceivedByUser(10L)).thenReturn(List.of());
    }

    @Test
    void balansiSuTacniIZbrajajuSeNaNulu() {
        when(groupAccess.membersOf(10L, 1L)).thenReturn(clanovi);
        trosakOdStoKM();

        List<BalanceResponse> balansi = balanceService.balances(1L, 10L);

        assertThat(balansi).extracting(BalanceResponse::displayName).containsExactly("Ana", "Emir", "Sara");
        assertThat(balansi).extracting(BalanceResponse::balance)
                .containsExactly(new BigDecimal("66.66"), new BigDecimal("-33.33"), new BigDecimal("-33.33"));
        assertThat(balansi.stream().map(BalanceResponse::balance).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("0");
    }

    @Test
    void prijedlogImaDvijeUplateAnaPrimaSve() {
        when(groupAccess.membersOf(10L, 1L)).thenReturn(clanovi);
        trosakOdStoKM();

        List<TransferResponse> prijedlog = balanceService.suggestSettlements(1L, 10L);

        assertThat(prijedlog).hasSize(2);
        assertThat(prijedlog).allSatisfy(t -> assertThat(t.to().displayName()).isEqualTo("Ana"));
        assertThat(prijedlog).extracting(TransferResponse::amount)
                .containsExactlyInAnyOrder(new BigDecimal("33.33"), new BigDecimal("33.33"));
    }

    @Test
    void nijeClanGrupeDobijaGreskuIUpitiSeNePozivaju() {
        when(groupAccess.membersOf(10L, 99L)).thenThrow(new GroupNotFoundException());

        assertThatThrownBy(() -> balanceService.balances(99L, 10L)).isInstanceOf(GroupNotFoundException.class);
        assertThatThrownBy(() -> balanceService.suggestSettlements(99L, 10L))
                .isInstanceOf(GroupNotFoundException.class);
    }
}
