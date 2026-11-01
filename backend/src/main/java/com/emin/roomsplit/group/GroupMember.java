package com.emin.roomsplit.group;

import java.time.Instant;

import com.emin.roomsplit.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    // @MapsId: kolona group_id je istovremeno dio ključa i strani ključ ka grupi
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    // STRING: u bazi piše "OWNER", pa promjena redoslijeda u enumu ne kvari podatke
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public GroupMember(Group group, User user, GroupRole role) {
        this.id = new GroupMemberId(group.getId(), user.getId());
        this.group = group;
        this.user = user;
        this.role = role;
        this.joinedAt = Instant.now();
    }
}
