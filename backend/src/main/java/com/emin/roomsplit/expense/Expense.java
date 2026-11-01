package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    // Autor unosa: samo on smije mijenjati i brisati trošak (platilac može biti neko drugi)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExpenseCategory category;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // mappedBy: strani ključ je u tabeli expense_shares (polje "expense"), ova strana ga samo čita.
    // cascade ALL + orphanRemoval: dijelovi žive i umiru s troškom (snimanje/brisanje/izmjena liste).
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter(AccessLevel.NONE)
    private List<ExpenseShare> shares = new ArrayList<>();

    public Expense(Group group, User paidBy, User createdBy, String description, BigDecimal amount,
            ExpenseCategory category, LocalDate expenseDate) {
        this.group = group;
        this.paidBy = paidBy;
        this.createdBy = createdBy;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.createdAt = Instant.now();
    }

    // Group i createdBy se nikad ne mijenjaju: trošak ostaje u istoj grupi i kod istog autora
    public void update(User paidBy, String description, BigDecimal amount, ExpenseCategory category,
            LocalDate expenseDate) {
        this.paidBy = paidBy;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
    }

    public List<ExpenseShare> getShares() {
        return Collections.unmodifiableList(shares);
    }

    public void addShare(User user, BigDecimal shareAmount) {
        shares.add(new ExpenseShare(this, user, shareAmount));
    }

    // Za izmjenu troška: stari dijelovi se brišu (orphanRemoval), pa se dodaju novi
    public void clearShares() {
        shares.clear();
    }
}
