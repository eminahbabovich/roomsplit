package com.emin.roomsplit.group;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetailsResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateGroupRequest request) {
        return groupService.create(userId(jwt), request);
    }

    @PostMapping("/join")
    public GroupDetailsResponse join(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody JoinGroupRequest request) {
        return groupService.join(userId(jwt), request);
    }

    @GetMapping
    public List<GroupSummaryResponse> myGroups(@AuthenticationPrincipal Jwt jwt) {
        return groupService.myGroups(userId(jwt));
    }

    @GetMapping("/{id}")
    public GroupDetailsResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return groupService.get(userId(jwt), id);
    }

    // Id korisnika uvijek iz tokena, nikad iz zahtjeva
    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
