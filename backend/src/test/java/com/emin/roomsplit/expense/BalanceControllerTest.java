package com.emin.roomsplit.expense;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.emin.roomsplit.config.SecurityConfig;
import com.emin.roomsplit.group.GroupNotFoundException;

@WebMvcTest({ BalanceController.class, SettlementController.class })
@Import(SecurityConfig.class)
class BalanceControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private BalanceService balanceService;

    @MockitoBean
    private SettlementService settlementService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    BalanceControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private RequestPostProcessor prijavljen(String id) {
        return jwt().jwt(token -> token.subject(id));
    }

    @Test
    void balansiBezPrijaveVracaju401() throws Exception {
        mockMvc.perform(get("/api/groups/10/balances")).andExpect(status().isUnauthorized());
    }

    @Test
    void balansiVracaju200() throws Exception {
        when(balanceService.balances(1L, 10L)).thenReturn(List.of(
                new BalanceResponse(1L, "Ana", new BigDecimal("66.66")),
                new BalanceResponse(2L, "Emir", new BigDecimal("-66.66"))));

        mockMvc.perform(get("/api/groups/10/balances").with(prijavljen("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value("Ana"))
                .andExpect(jsonPath("$[1].balance").value(-66.66));
    }

    @Test
    void balansiTudjeGrupeVracaju404() throws Exception {
        when(balanceService.balances(5L, 10L)).thenThrow(new GroupNotFoundException());

        mockMvc.perform(get("/api/groups/10/balances").with(prijavljen("5"))).andExpect(status().isNotFound());
    }

    @Test
    void prijedlogPoravnanjaVraca200() throws Exception {
        when(balanceService.suggestSettlements(1L, 10L)).thenReturn(List.of(
                new TransferResponse(new PersonResponse(2L, "Emir"), new PersonResponse(1L, "Ana"),
                        new BigDecimal("30.00"))));

        mockMvc.perform(get("/api/groups/10/settle-suggestions").with(prijavljen("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].from.displayName").value("Emir"))
                .andExpect(jsonPath("$[0].amount").value(30.00));
    }

    @Test
    void uplataVraca201() throws Exception {
        when(settlementService.record(eq(2L), eq(10L), any(SettlementRequest.class))).thenReturn(
                new SettlementResponse(7L, new PersonResponse(2L, "Emir"), new PersonResponse(1L, "Ana"),
                        new BigDecimal("30.00"), Instant.parse("2026-11-01T12:00:00Z")));

        mockMvc.perform(post("/api/groups/10/settlements").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"toUser\":1,\"amount\":30.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.to.displayName").value("Ana"));
    }

    @Test
    void uplataSNevalidnimIznosomVraca400() throws Exception {
        mockMvc.perform(post("/api/groups/10/settlements").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"toUser\":1,\"amount\":0.001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void uplataBezPoljaVraca400() throws Exception {
        mockMvc.perform(post("/api/groups/10/settlements").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.toUser").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }
}
