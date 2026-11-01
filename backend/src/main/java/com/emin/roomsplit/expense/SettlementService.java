package com.emin.roomsplit.expense;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.common.InvalidRequestException;
import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupAccess;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final GroupAccess groupAccess;
    private final GroupRepository groupRepository;
    private final SettlementRepository settlementRepository;

    // Prijavljeni korisnik bilježi da je on isplatio nekome iznos amount. Ne može se bilježiti uplata
    // u tuđe ime. Iznos ne mora tačno odgovarati dugu: veća uplata od duga pretvara primaoca u dužnika.
    @Transactional
    public SettlementResponse record(Long userId, Long groupId, SettlementRequest request) {
        Map<Long, User> members = groupAccess.membersOf(groupId, userId);

        if (request.toUser().equals(userId)) {
            throw new InvalidRequestException("Ne možeš zabilježiti uplatu samom sebi");
        }
        User recipient = members.get(request.toUser());
        if (recipient == null) {
            throw new InvalidRequestException("Primalac uplate nije član grupe");
        }

        Group group = groupRepository.getReferenceById(groupId);
        Settlement saved = settlementRepository.save(
                new Settlement(group, members.get(userId), recipient, request.amount()));
        return SettlementResponse.from(saved);
    }
}
