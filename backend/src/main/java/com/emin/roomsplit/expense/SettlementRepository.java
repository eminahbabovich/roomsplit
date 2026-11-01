package com.emin.roomsplit.expense;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // Koliko je koji korisnik ukupno isplatio drugima
    @Query("""
            select new com.emin.roomsplit.expense.UserTotal(s.fromUser.id, sum(s.amount))
            from Settlement s
            where s.group.id = :groupId
            group by s.fromUser.id
            """)
    List<UserTotal> sumPaidOutByUser(@Param("groupId") Long groupId);

    // Koliko je koji korisnik ukupno primio od drugih
    @Query("""
            select new com.emin.roomsplit.expense.UserTotal(s.toUser.id, sum(s.amount))
            from Settlement s
            where s.group.id = :groupId
            group by s.toUser.id
            """)
    List<UserTotal> sumReceivedByUser(@Param("groupId") Long groupId);
}
