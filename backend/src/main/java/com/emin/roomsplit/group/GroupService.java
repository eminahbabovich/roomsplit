package com.emin.roomsplit.group;

import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserNotFoundException;
import com.emin.roomsplit.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final int MAX_CODE_ATTEMPTS = 5;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final JoinCodeGenerator joinCodeGenerator;

    // Dva upisa (grupa i prvi član) moraju uspjeti zajedno: grupa bez vlasnika bi bila "siroče"
    @Transactional
    public GroupDetailsResponse create(Long userId, CreateGroupRequest request) {
        User user = findUser(userId);
        Group group = groupRepository.save(new Group(request.name().trim(), uniqueJoinCode(), user));
        groupMemberRepository.save(new GroupMember(group, user, GroupRole.OWNER));
        return details(group);
    }

    @Transactional
    public GroupDetailsResponse join(Long userId, JoinGroupRequest request) {
        String code = request.joinCode().trim().toUpperCase(Locale.ROOT);
        Group group = groupRepository.findByJoinCode(code).orElseThrow(GroupNotFoundException::new);

        if (groupMemberRepository.existsByIdGroupIdAndIdUserId(group.getId(), userId)) {
            throw new AlreadyGroupMemberException();
        }

        try {
            groupMemberRepository.saveAndFlush(new GroupMember(group, findUser(userId), GroupRole.MEMBER));
        } catch (DataIntegrityViolationException e) {
            // Dva istovremena zahtjeva istog korisnika: primarni ključ (group_id, user_id) uhvati drugi
            throw new AlreadyGroupMemberException();
        }
        return details(group);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> myGroups(Long userId) {
        return groupMemberRepository.findAllByUserIdWithGroup(userId).stream()
                .map(GroupSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailsResponse get(Long userId, Long groupId) {
        // Autorizacija: prvo provjera članstva, tek onda učitavanje grupe
        if (!groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
            throw new GroupNotFoundException();
        }
        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        return details(group);
    }

    private GroupDetailsResponse details(Group group) {
        return GroupDetailsResponse.from(group, groupMemberRepository.findAllByGroupIdWithUser(group.getId()));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private String uniqueJoinCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = joinCodeGenerator.generate();
            if (!groupRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Nije uspjelo generisanje jedinstvenog koda za grupu");
    }
}
