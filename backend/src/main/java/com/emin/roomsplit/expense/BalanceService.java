package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.group.GroupAccess;
import com.emin.roomsplit.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceService {

    private final GroupAccess groupAccess;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SettlementRepository settlementRepository;

    public List<BalanceResponse> balances(Long userId, Long groupId) {
        Map<Long, User> members = groupAccess.membersOf(groupId, userId);
        Map<Long, BigDecimal> balances = computeBalances(groupId, members);

        return members.values().stream()
                .map(user -> new BalanceResponse(user.getId(), user.getDisplayName(), balances.get(user.getId())))
                .toList();
    }

    public List<TransferResponse> suggestSettlements(Long userId, Long groupId) {
        Map<Long, User> members = groupAccess.membersOf(groupId, userId);

        // Algoritam radi s cijelim brojevima feninga, bez BigDecimal-a
        Map<Long, Long> feninzi = computeBalances(groupId, members).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().movePointRight(2).longValueExact(),
                        (a, b) -> a, LinkedHashMap::new));

        return SettlementPlanner.plan(feninzi).stream()
                .map(transfer -> new TransferResponse(
                        PersonResponse.from(members.get(transfer.fromUserId())),
                        PersonResponse.from(members.get(transfer.toUserId())),
                        BigDecimal.valueOf(transfer.feninzi(), 2)))
                .toList();
    }

    // Četiri zbirna upita (SUM + GROUP BY) umjesto učitavanja svih troškova i uplata u memoriju
    private Map<Long, BigDecimal> computeBalances(Long groupId, Map<Long, User> members) {
        return BalanceCalculator.calculate(members.keySet(),
                toMap(expenseRepository.sumPaidByUser(groupId)),
                toMap(expenseShareRepository.sumOwedByUser(groupId)),
                toMap(settlementRepository.sumPaidOutByUser(groupId)),
                toMap(settlementRepository.sumReceivedByUser(groupId)));
    }

    private Map<Long, BigDecimal> toMap(List<UserTotal> totals) {
        return totals.stream().collect(Collectors.toMap(UserTotal::userId, UserTotal::total));
    }
}
