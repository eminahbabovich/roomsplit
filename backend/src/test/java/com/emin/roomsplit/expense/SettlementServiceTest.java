package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.emin.roomsplit.common.InvalidRequestException;
import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupAccess;
import com.emin.roomsplit.group.GroupNotFoundException;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.user.User;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private GroupAccess groupAccess;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SettlementService settlementService;

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
    }

    @Test
    void uplataSeBiljeziOdPrijavljenogKorisnika() {
        when(groupAccess.membersOf(10L, 2L)).thenReturn(clanovi);
        when(groupRepository.getReferenceById(10L)).thenReturn(new Group("Stan", "ABC12345", clanovi.get(1L)));
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(inv -> inv.getArgument(0));

        SettlementResponse odgovor = settlementService.record(2L, 10L, new SettlementRequest(1L, new BigDecimal("30.00")));

        assertThat(odgovor.from().displayName()).isEqualTo("Emir");
        assertThat(odgovor.to().displayName()).isEqualTo("Ana");
        assertThat(odgovor.amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void uplataSamomSebiSeOdbija() {
        when(groupAccess.membersOf(10L, 2L)).thenReturn(clanovi);

        assertThatThrownBy(() -> settlementService.record(2L, 10L, new SettlementRequest(2L, new BigDecimal("5.00"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void primalacKojiNijeClanSeOdbija() {
        when(groupAccess.membersOf(10L, 2L)).thenReturn(clanovi);

        assertThatThrownBy(() -> settlementService.record(2L, 10L, new SettlementRequest(99L, new BigDecimal("5.00"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void nijeClanGrupeNeMozeBiljezitiUplatu() {
        when(groupAccess.membersOf(10L, 99L)).thenThrow(new GroupNotFoundException());

        assertThatThrownBy(() -> settlementService.record(99L, 10L, new SettlementRequest(1L, new BigDecimal("5.00"))))
                .isInstanceOf(GroupNotFoundException.class);
        verify(settlementRepository, never()).save(any());
    }
}
