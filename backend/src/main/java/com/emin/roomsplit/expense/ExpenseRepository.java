package com.emin.roomsplit.expense;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // join fetch samo za veze "prema jednom" (platilac, autor): to je sigurno uz paginaciju.
    // Da smo ovdje fetch-ovali i listu dijelova, Hibernate bi paginirao u memoriji.
    @Query(value = """
            select e from Expense e
            join fetch e.paidBy
            join fetch e.createdBy
            where e.group.id = :groupId
              and e.expenseDate between :from and :to
              and e.category in :categories
            order by e.expenseDate desc, e.id desc
            """,
            countQuery = """
            select count(e) from Expense e
            where e.group.id = :groupId
              and e.expenseDate between :from and :to
              and e.category in :categories
            """)
    Page<Expense> search(@Param("groupId") Long groupId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("categories") Collection<ExpenseCategory> categories,
            Pageable pageable);

    // Zbir plaćenog po korisniku računa baza (SUM + GROUP BY), ne učitavamo sve troškove u memoriju
    @Query("""
            select new com.emin.roomsplit.expense.UserTotal(e.paidBy.id, sum(e.amount))
            from Expense e
            where e.group.id = :groupId
            group by e.paidBy.id
            """)
    List<UserTotal> sumPaidByUser(@Param("groupId") Long groupId);

    @Query("""
            select e from Expense e
            join fetch e.paidBy
            join fetch e.createdBy
            where e.id = :id and e.group.id = :groupId
            """)
    Optional<Expense> findByIdAndGroupIdWithUsers(@Param("id") Long id, @Param("groupId") Long groupId);
}
