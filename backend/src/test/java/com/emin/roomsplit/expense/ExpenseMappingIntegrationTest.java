package com.emin.roomsplit.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupMember;
import com.emin.roomsplit.group.GroupMemberRepository;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.group.GroupRole;
import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;

import jakarta.persistence.EntityManager;

// Provjerava migraciju V3 i mapiranje s pravom bazom: spremanje i čitanje troška s dijelovima,
// te da baza sama odbija nevaljane podatke (iznos <= 0, platilac koji nije član grupe).
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExpenseMappingIntegrationTest {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final EntityManager entityManager;

    @Autowired
    ExpenseMappingIntegrationTest(UserRepository userRepository, GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository, ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.entityManager = entityManager;
    }

    private User noviKorisnik(String ime) {
        return userRepository.save(new User(ime + "-" + UUID.randomUUID() + "@example.com", "HASH", ime));
    }

    private Group grupaSaClanovima(User vlasnik, User... ostali) {
        Group group = groupRepository.save(new Group("Stan", UUID.randomUUID().toString().substring(0, 8), vlasnik));
        groupMemberRepository.save(new GroupMember(group, vlasnik, GroupRole.OWNER));
        for (User user : ostali) {
            groupMemberRepository.save(new GroupMember(group, user, GroupRole.MEMBER));
        }
        return group;
    }

    private Expense trosak(Group group, User platio, User autor, String iznos) {
        return new Expense(group, platio, autor, "Namirnice", new BigDecimal(iznos),
                ExpenseCategory.FOOD, LocalDate.of(2026, 11, 1));
    }

    @Test
    void trosakSDijelovimaSeSpremaIVracaIzBaze() {
        User ana = noviKorisnik("Ana");
        User emir = noviKorisnik("Emir");
        Group group = grupaSaClanovima(ana, emir);

        Expense expense = trosak(group, ana, ana, "100.00");
        expense.addShare(ana, new BigDecimal("50.00"));
        expense.addShare(emir, new BigDecimal("50.00"));
        Long id = expenseRepository.saveAndFlush(expense).getId();
        entityManager.clear(); // prisili čitanje iz baze, ne iz keša

        Expense procitan = expenseRepository.findById(id).orElseThrow();

        assertThat(procitan.getAmount()).isEqualByComparingTo("100.00");
        assertThat(procitan.getCategory()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(procitan.getShares()).hasSize(2);
        BigDecimal zbirDijelova = procitan.getShares().stream()
                .map(ExpenseShare::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(zbirDijelova).isEqualByComparingTo(procitan.getAmount());
    }

    @Test
    void brisanjeTroskaBriseINjegoveDijelove() {
        User ana = noviKorisnik("Ana");
        Group group = grupaSaClanovima(ana);
        Expense expense = trosak(group, ana, ana, "20.00");
        expense.addShare(ana, new BigDecimal("20.00"));
        Long id = expenseRepository.saveAndFlush(expense).getId();

        expenseRepository.deleteById(id);
        expenseRepository.flush();
        entityManager.clear();

        Number preostalihDijelova = (Number) entityManager
                .createQuery("select count(s) from ExpenseShare s where s.expense.id = :id")
                .setParameter("id", id)
                .getSingleResult();
        assertThat(preostalihDijelova.longValue()).isZero();
    }

    @Test
    void bazaOdbijaTrosakSaNultimIznosom() {
        User ana = noviKorisnik("Ana");
        Group group = grupaSaClanovima(ana);

        assertThatThrownBy(() -> expenseRepository.saveAndFlush(trosak(group, ana, ana, "0.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bazaOdbijaPlatiocaKojiNijeClanGrupe() {
        User ana = noviKorisnik("Ana");
        User stranac = noviKorisnik("Stranac");
        Group group = grupaSaClanovima(ana);

        assertThatThrownBy(() -> expenseRepository.saveAndFlush(trosak(group, stranac, ana, "10.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bazaOdbijaUplatuSamomSebi() {
        User ana = noviKorisnik("Ana");
        Group group = grupaSaClanovima(ana);

        assertThatThrownBy(() -> settlementRepository.saveAndFlush(
                new Settlement(group, ana, ana, new BigDecimal("5.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uplataIzmeduClanovaSeSprema() {
        User ana = noviKorisnik("Ana");
        User emir = noviKorisnik("Emir");
        Group group = grupaSaClanovima(ana, emir);

        Settlement settlement = settlementRepository.saveAndFlush(
                new Settlement(group, emir, ana, new BigDecimal("12.50")));

        assertThat(settlement.getId()).isNotNull();
    }
}
