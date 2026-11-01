package com.emin.roomsplit.group;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    boolean existsByIdGroupIdAndIdUserId(Long groupId, Long userId);

    // join fetch učitava grupu istim upitom; bez njega bi svaki red radio dodatni SELECT (N+1 problem)
    @Query("select m from GroupMember m join fetch m.group where m.id.userId = :userId order by m.group.name")
    List<GroupMember> findAllByUserIdWithGroup(@Param("userId") Long userId);

    @Query("select m from GroupMember m join fetch m.user where m.id.groupId = :groupId order by m.joinedAt")
    List<GroupMember> findAllByGroupIdWithUser(@Param("groupId") Long groupId);
}
