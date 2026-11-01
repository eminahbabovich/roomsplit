package com.emin.roomsplit.group;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);
}
