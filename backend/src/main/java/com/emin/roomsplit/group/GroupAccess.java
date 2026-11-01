package com.emin.roomsplit.group;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.emin.roomsplit.user.User;

import lombok.RequiredArgsConstructor;

// Jedno mjesto za provjeru "da li korisnik smije vidjeti ovu grupu", da je ne ponavljamo u svakom servisu
@Component
@RequiredArgsConstructor
public class GroupAccess {

    private final GroupMemberRepository groupMemberRepository;

    // Članovi grupe (id -> korisnik), redom po datumu pristupanja. Ko nije član, dobija isti 404
    // kao da grupa ne postoji.
    public Map<Long, User> membersOf(Long groupId, Long userId) {
        Map<Long, User> members = new LinkedHashMap<>();
        groupMemberRepository.findAllByGroupIdWithUser(groupId)
                .forEach(member -> members.put(member.getId().getUserId(), member.getUser()));
        if (!members.containsKey(userId)) {
            throw new GroupNotFoundException();
        }
        return members;
    }
}
