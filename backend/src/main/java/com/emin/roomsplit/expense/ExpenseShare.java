package com.emin.roomsplit.expense;

import java.math.BigDecimal;

import com.emin.roomsplit.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false, updatable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "share_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shareAmount;

    // Package-private: dio se pravi samo kroz Expense.addShare, da veza uvijek bude dvosmjerno ispravna
    ExpenseShare(Expense expense, User user, BigDecimal shareAmount) {
        this.expense = expense;
        this.user = user;
        this.shareAmount = shareAmount;
    }
}
