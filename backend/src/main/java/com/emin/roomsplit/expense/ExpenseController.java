package com.emin.roomsplit.expense;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.emin.roomsplit.common.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId,
            @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(userId(jwt), groupId, request);
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ExpenseCategory category) {
        return expenseService.list(userId(jwt), groupId, page, size, from, to, category);
    }

    @GetMapping("/{expenseId}")
    public ExpenseResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId,
            @PathVariable Long expenseId) {
        return expenseService.get(userId(jwt), groupId, expenseId);
    }

    @PutMapping("/{expenseId}")
    public ExpenseResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId,
            @PathVariable Long expenseId, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(userId(jwt), groupId, expenseId, request);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long groupId, @PathVariable Long expenseId) {
        expenseService.delete(userId(jwt), groupId, expenseId);
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
