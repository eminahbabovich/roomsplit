package com.emin.roomsplit.group;

import java.time.Instant;
import java.util.List;

public record GroupDetailsResponse(Long id, String name, String joinCode, Instant createdAt, List<MemberResponse> members) {

    public static GroupDetailsResponse from(Group group, List<GroupMember> members) {
        return new GroupDetailsResponse(
                group.getId(),
                group.getName(),
                group.getJoinCode(),
                group.getCreatedAt(),
                members.stream().map(MemberResponse::from).toList());
    }
}
