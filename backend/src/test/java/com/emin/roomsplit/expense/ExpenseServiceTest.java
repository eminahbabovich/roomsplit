package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.emin.roomsplit.common.InvalidRequestException;
import com.emin.roomsplit.common.PageResponse;
import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupMember;
import com.emin.roomsplit.group.GroupMemberRepository;
import com.emin.roomsplit.group.GroupNotFoundException;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.group.GroupRole;
import com.emin.roomsplit.user.User;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final LocalDate DATUM = LocalDate.of(2026, 11, 1);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseShareRepository expenseShareRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRepository groupRepository;

    @InjectMocks
    private ExpenseService expenseService;

    private User ana;
    private User emir;
    private User sara;
    private Group grupa;

    @BeforeEach
    void postavke() {
        ana = korisnik(1L, "Ana");
        emir = korisnik(2L, "Emir");
        sara = korisnik(3L, "Sara");
        grupa = new Group("Stan", "ABCD2345", ana);
        ReflectionTestUtils.setField(grupa, "id", 10L);
    }

    private User korisnik(long id, String ime) {
        User user = new User(ime.toLowerCase() + "@example.com", "HASH", ime);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    // Grupa 10 ima članove ana, emir i sara
    private void clanoviGrupe() {
        when(groupMemberRepository.findAllByGroupIdWithUser(10L)).thenReturn(List.of(
                new GroupMember(grupa, ana, GroupRole.OWNER),
                new GroupMember(grupa, emir, GroupRole.MEMBER),
                new GroupMember(grupa, sara, GroupRole.MEMBER)));
    }

    private ExpenseRequest jednakaPodjela(String iznos, Long platio, Long... ucesnici) {
        return new ExpenseRequest(" Namirnice ", bd(iznos), platio, ExpenseCategory.FOOD, DATUM, SplitType.EQUAL,
                java.util.Arrays.stream(ucesnici).map(id -> new ParticipantRequest(id, null)).toList());
    }

    private Expense postojeciTrosak(User autor) {
        Expense expense = new Expense(grupa, autor, autor, "Struja", bd("100.00"), ExpenseCategory.UTILITIES, DATUM);
        expense.addShare(ana, bd("100.00"));
        return expense;
    }

    // --- kreiranje ---

    @Test
    void jednakaPodjelaDajeOstatakPrvomUcesniku() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.create(1L, 10L, jednakaPodjela("100.00", 1L, 1L, 2L, 3L));

        assertThat(response.description()).isEqualTo("Namirnice");
        assertThat(response.paidBy().userId()).isEqualTo(1L);
        assertThat(response.createdBy().userId()).isEqualTo(1L);
        assertThat(response.shares()).extracting(ShareResponse::amount)
                .containsExactly(bd("33.34"), bd("33.33"), bd("33.33"));
    }

    @Test
    void platilacMozeBitiDrugaOsobaOdAutora() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.create(1L, 10L, jednakaPodjela("30.00", 2L, 1L, 2L, 3L));

        assertThat(response.paidBy().userId()).isEqualTo(2L);
        assertThat(response.createdBy().userId()).isEqualTo(1L);
    }

    @Test
    void dioOdNulaFeningaSeNeUpisuje() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseResponse response = expenseService.create(1L, 10L, jednakaPodjela("0.01", 1L, 1L, 2L, 3L));

        assertThat(response.shares()).singleElement()
                .satisfies(dio -> assertThat(dio.userId()).isEqualTo(1L));
    }

    @Test
    void tacniIznosiSePrihvataju() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        ExpenseRequest request = new ExpenseRequest("Večera", bd("100.00"), 1L, null, DATUM, SplitType.EXACT,
                List.of(new ParticipantRequest(1L, bd("60.00")), new ParticipantRequest(2L, bd("40.00"))));

        ExpenseResponse response = expenseService.create(1L, 10L, request);

        assertThat(response.shares()).extracting(ShareResponse::amount).containsExactly(bd("60.00"), bd("40.00"));
        assertThat(response.category()).isEqualTo(ExpenseCategory.OTHER);
    }

    @Test
    void tacniIznosiBezIznosaZaUcesnikaSuOdbijeni() {
        clanoviGrupe();
        ExpenseRequest request = new ExpenseRequest("Večera", bd("100.00"), 1L, null, DATUM, SplitType.EXACT,
                List.of(new ParticipantRequest(1L, bd("100.00")), new ParticipantRequest(2L, null)));

        assertThatThrownBy(() -> expenseService.create(1L, 10L, request)).isInstanceOf(InvalidSplitException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void tacniIznosiKojiNePogadjajuZbirSuOdbijeni() {
        clanoviGrupe();
        ExpenseRequest request = new ExpenseRequest("Večera", bd("100.00"), 1L, null, DATUM, SplitType.EXACT,
                List.of(new ParticipantRequest(1L, bd("60.00")), new ParticipantRequest(2L, bd("39.99"))));

        assertThatThrownBy(() -> expenseService.create(1L, 10L, request)).isInstanceOf(InvalidSplitException.class);
    }

    @Test
    void platilacKojiNijeClanGrupeJeOdbijen() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);

        assertThatThrownBy(() -> expenseService.create(1L, 10L, jednakaPodjela("10.00", 99L, 1L, 2L)))
                .isInstanceOf(InvalidSplitException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void ucesnikKojiNijeClanGrupeJeOdbijen() {
        clanoviGrupe();
        when(groupRepository.getReferenceById(10L)).thenReturn(grupa);

        assertThatThrownBy(() -> expenseService.create(1L, 10L, jednakaPodjela("10.00", 1L, 1L, 99L)))
                .isInstanceOf(InvalidSplitException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    @Test
    void nekoKoNijeClanGrupeNeMozeDodatiTrosak() {
        clanoviGrupe();

        assertThatThrownBy(() -> expenseService.create(77L, 10L, jednakaPodjela("10.00", 1L, 1L)))
                .isInstanceOf(GroupNotFoundException.class);

        verify(expenseRepository, never()).save(any(Expense.class));
    }

    // --- izmjena i brisanje ---

    @Test
    void trosakMozeMijenjatiSamoAutor() {
        clanoviGrupe();
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.of(postojeciTrosak(ana)));

        assertThatThrownBy(() -> expenseService.update(2L, 10L, 5L, jednakaPodjela("50.00", 1L, 1L, 2L)))
                .isInstanceOf(NotExpenseAuthorException.class);

        verify(expenseRepository, never()).saveAndFlush(any(Expense.class));
    }

    @Test
    void autorMijenjaTrosakIDijeloviSeZamjenjuju() {
        clanoviGrupe();
        Expense expense = postojeciTrosak(ana);
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.of(expense));
        ExpenseRequest request = new ExpenseRequest("Struja i voda", bd("100.00"), 2L, ExpenseCategory.UTILITIES,
                DATUM, SplitType.EXACT,
                List.of(new ParticipantRequest(1L, bd("40.00")), new ParticipantRequest(2L, bd("60.00"))));

        ExpenseResponse response = expenseService.update(1L, 10L, 5L, request);

        assertThat(response.description()).isEqualTo("Struja i voda");
        assertThat(response.paidBy().userId()).isEqualTo(2L);
        assertThat(response.shares()).extracting(ShareResponse::amount).containsExactly(bd("40.00"), bd("60.00"));
        // Stari dijelovi moraju biti obrisani (flush) prije upisa novih zbog UNIQUE (expense_id, user_id)
        verify(expenseRepository).saveAndFlush(expense);
    }

    @Test
    void nevaljanaIzmjenaNeMijenjaTrosak() {
        clanoviGrupe();
        Expense expense = postojeciTrosak(ana);
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.update(1L, 10L, 5L, jednakaPodjela("0.00", 1L, 1L)))
                .isInstanceOf(InvalidSplitException.class);

        assertThat(expense.getAmount()).isEqualByComparingTo("100.00");
        verify(expenseRepository, never()).saveAndFlush(any(Expense.class));
    }

    @Test
    void trosakBriseSamoAutor() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 2L)).thenReturn(true);
        Expense expense = postojeciTrosak(ana);
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> expenseService.delete(2L, 10L, 5L)).isInstanceOf(NotExpenseAuthorException.class);

        verify(expenseRepository, never()).delete(any(Expense.class));
    }

    @Test
    void autorBriseTrosak() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);
        Expense expense = postojeciTrosak(ana);
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.of(expense));

        expenseService.delete(1L, 10L, 5L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void nepostojeciTrosakDajeNotFound() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(expenseRepository.findByIdAndGroupIdWithUsers(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.get(1L, 10L, 5L)).isInstanceOf(ExpenseNotFoundException.class);
    }

    // --- lista ---

    @Test
    void listaOdbijaPocetniDatumPoslijeKrajnjeg() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> expenseService.list(1L, 10L, 0, 20, LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 1), null)).isInstanceOf(InvalidRequestException.class);

        verify(expenseRepository, never()).search(any(), any(), any(), any(), any());
    }

    @Test
    void listaOgranicavaVelicinuStraniceIUzimaSveKategorijeKadFilterNijeZadan() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 1L)).thenReturn(true);
        when(expenseRepository.search(eq(10L), any(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<Expense>(List.of(), inv.getArgument(4), 0));

        PageResponse<ExpenseResponse> response = expenseService.list(1L, 10L, -5, 1000, null, null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<ExpenseCategory>> kategorije = ArgumentCaptor.forClass(Collection.class);
        verify(expenseRepository).search(eq(10L), eq(LocalDate.of(1900, 1, 1)), eq(LocalDate.of(9999, 12, 31)),
                kategorije.capture(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(kategorije.getValue()).containsExactlyInAnyOrder(ExpenseCategory.values());
        assertThat(response.items()).isEmpty();
    }

    @Test
    void nekoKoNijeClanNeMozeVidjetiListu() {
        when(groupMemberRepository.existsByIdGroupIdAndIdUserId(10L, 77L)).thenReturn(false);

        assertThatThrownBy(() -> expenseService.list(77L, 10L, 0, 20, null, null, null))
                .isInstanceOf(GroupNotFoundException.class);
    }
}
