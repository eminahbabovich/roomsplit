package com.emin.roomsplit.expense;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups/{groupId}")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/balances")
    public List<BalanceResponse> balances(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return balanceService.balances(Long.valueOf(jwt.getSubject()), groupId);
    }

    @GetMapping("/settle-suggestions")
    public List<TransferResponse> settleSuggestions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId) {
        return balanceService.suggestSettlements(Long.valueOf(jwt.getSubject()), groupId);
    }
}
