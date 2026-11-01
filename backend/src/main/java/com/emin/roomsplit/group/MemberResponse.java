package com.emin.roomsplit.group;

import java.time.Instant;

// Članovi vide jedni drugima samo ime za prikaz, ne email
public record MemberResponse(Long userId, String displayName, GroupRole role, Instant joinedAt) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(
                member.getId().getUserId(),
                member.getUser().getDisplayName(),
                member.getRole(),
                member.getJoinedAt());
    }
}
