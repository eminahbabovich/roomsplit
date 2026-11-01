package com.emin.roomsplit.group;

// Kratak prikaz grupe za listu "moje grupe", uz ulogu prijavljenog korisnika u njoj
public record GroupSummaryResponse(Long id, String name, GroupRole role) {

    public static GroupSummaryResponse from(GroupMember member) {
        return new GroupSummaryResponse(member.getGroup().getId(), member.getGroup().getName(), member.getRole());
    }
}
