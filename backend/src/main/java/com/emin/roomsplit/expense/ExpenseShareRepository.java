package com.emin.roomsplit.expense;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseShareRepository extends JpaRepository<ExpenseShare, Long> {

    // Dijelovi svih troškova sa stranice jednim upitom (umjesto jednog upita po trošku)
    @Query("select s from ExpenseShare s join fetch s.user where s.expense.id in :expenseIds order by s.id")
    List<ExpenseShare> findAllByExpenseIds(@Param("expenseIds") Collection<Long> expenseIds);
}
