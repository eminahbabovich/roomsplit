package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.common.InvalidRequestException;
import com.emin.roomsplit.common.PageResponse;
import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupMemberRepository;
import com.emin.roomsplit.group.GroupNotFoundException;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;
    // Zamjena za "bez ograničenja" kod filtriranja po datumu (izbjegava null parametre u upitu)
    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;

    @Transactional
    public ExpenseResponse create(Long userId, Long groupId, ExpenseRequest request) {
        Map<Long, User> members = membersOf(groupId, userId);
        Map<Long, BigDecimal> shares = calculateShares(request);
        Group group = groupRepository.getReferenceById(groupId);

        Expense expense = new Expense(group, memberOrFail(members, request.paidBy(), "Platilac"), members.get(userId),
                request.description().trim(), request.amount(), categoryOrDefault(request), request.expenseDate());
        addShares(expense, shares, members);

        Expense saved = expenseRepository.save(expense);
        return ExpenseResponse.from(saved, saved.getShares());
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long userId, Long groupId, Long expenseId) {
        requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        return ExpenseResponse.from(expense, expenseShareRepository.findAllByExpenseIds(List.of(expenseId)));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(Long userId, Long groupId, int page, int size,
            LocalDate from, LocalDate to, ExpenseCategory category) {
        requireMember(groupId, userId);

        LocalDate fromDate = from != null ? from : MIN_DATE;
        LocalDate toDate = to != null ? to : MAX_DATE;
        if (fromDate.isAfter(toDate)) {
            throw new InvalidRequestException("Početni datum ne može biti poslije krajnjeg");
        }
        List<ExpenseCategory> categories = category != null ? List.of(category) : List.of(ExpenseCategory.values());
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        Page<Expense> result = expenseRepository.search(groupId, fromDate, toDate, categories, pageRequest);

        // Jedan dodatni upit za dijelove svih troškova sa stranice
        Map<Long, List<ExpenseShare>> sharesByExpense = result.isEmpty() ? Map.of()
                : expenseShareRepository.findAllByExpenseIds(result.stream().map(Expense::getId).toList()).stream()
                        .collect(Collectors.groupingBy(share -> share.getExpense().getId()));

        List<ExpenseResponse> items = result.stream()
                .map(expense -> ExpenseResponse.from(expense, sharesByExpense.getOrDefault(expense.getId(), List.of())))
                .toList();
        return PageResponse.of(result, items);
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long groupId, Long expenseId, ExpenseRequest request) {
        Map<Long, User> members = membersOf(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        requireAuthor(expense, userId);

        // Sve se provjeri prije prve izmjene, pa nevaljan zahtjev ne ostavi trošak napola promijenjen
        Map<Long, BigDecimal> shares = calculateShares(request);
        User paidBy = memberOrFail(members, request.paidBy(), "Platilac");

        expense.update(paidBy, request.description().trim(), request.amount(), categoryOrDefault(request),
                request.expenseDate());
        expense.clearShares();
        // flush: stari dijelovi se moraju obrisati prije upisa novih, inače UNIQUE (expense_id, user_id)
        // puca (Hibernate inače radi INSERT prije DELETE)
        expenseRepository.saveAndFlush(expense);
        addShares(expense, shares, members);

        return ExpenseResponse.from(expense, expense.getShares());
    }

    @Transactional
    public void delete(Long userId, Long groupId, Long expenseId) {
        requireMember(groupId, userId);
        Expense expense = findExpense(groupId, expenseId);
        requireAuthor(expense, userId);
        expenseRepository.delete(expense);
    }

    // --- pomoćne metode ---

    // Članovi grupe (id -> korisnik); ko nije član, dobija isti 404 kao da grupa ne postoji
    private Map<Long, User> membersOf(Long groupId, Long userId) {
        Map<Long, User> members = new HashMap<>();
        groupMemberRepository.findAllByGroupIdWithUser(groupId)
                .forEach(member -> members.put(member.getId().getUserId(), member.getUser()));
        if (!members.containsKey(userId)) {
            throw new GroupNotFoundException();
        }
        return members;
    }

    private void requireMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
            throw new GroupNotFoundException();
        }
    }

    private Expense findExpense(Long groupId, Long expenseId) {
        return expenseRepository.findByIdAndGroupIdWithUsers(expenseId, groupId)
                .orElseThrow(ExpenseNotFoundException::new);
    }

    private void requireAuthor(Expense expense, Long userId) {
        if (!expense.getCreatedBy().getId().equals(userId)) {
            throw new NotExpenseAuthorException();
        }
    }

    private User memberOrFail(Map<Long, User> members, Long userId, String uloga) {
        User user = members.get(userId);
        if (user == null) {
            throw new InvalidSplitException(uloga + " (korisnik " + userId + ") nije član grupe");
        }
        return user;
    }

    private ExpenseCategory categoryOrDefault(ExpenseRequest request) {
        return request.category() != null ? request.category() : ExpenseCategory.OTHER;
    }

    private Map<Long, BigDecimal> calculateShares(ExpenseRequest request) {
        List<Long> userIds = request.participants().stream().map(ParticipantRequest::userId).toList();

        if (request.splitType() == SplitType.EQUAL) {
            return ExpenseSplitter.splitEqually(request.amount(), userIds);
        }

        Map<Long, BigDecimal> exact = new LinkedHashMap<>();
        for (ParticipantRequest participant : request.participants()) {
            if (participant.amount() == null) {
                throw new InvalidSplitException("Kod podjele EXACT svaki učesnik mora imati iznos");
            }
            if (exact.put(participant.userId(), participant.amount()) != null) {
                throw new InvalidSplitException("Isti učesnik se ne smije navesti dvaput");
            }
        }
        return ExpenseSplitter.validateExact(request.amount(), exact);
    }

    // Dio od 0.00 se ne upisuje (npr. 0.01 KM na troje): baza ionako traži share_amount > 0
    private void addShares(Expense expense, Map<Long, BigDecimal> shares, Map<Long, User> members) {
        shares.forEach((userId, amount) -> {
            User user = memberOrFail(members, userId, "Učesnik");
            if (amount.signum() > 0) {
                expense.addShare(user, amount);
            }
        });
    }
}
