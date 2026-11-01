package com.emin.roomsplit.expense;

import java.math.BigDecimal;
import java.time.Instant;

import com.emin.roomsplit.group.Group;
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

// Zabilježena uplata: fromUser je isplatio toUser iznos amount (smanjuje dug između njih)
@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user", nullable = false, updatable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user", nullable = false, updatable = false)
    private User toUser;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "settled_at", nullable = false, updatable = false)
    private Instant settledAt;

    public Settlement(Group group, User fromUser, User toUser, BigDecimal amount) {
        this.group = group;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
        this.settledAt = Instant.now();
    }
}
