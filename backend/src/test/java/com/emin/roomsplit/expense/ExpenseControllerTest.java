package com.emin.roomsplit.expense;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

import com.emin.roomsplit.common.PageResponse;
import com.emin.roomsplit.group.GroupNotFoundException;
import com.emin.roomsplit.config.SecurityConfig;

@WebMvcTest(ExpenseController.class)
@Import(SecurityConfig.class)
class ExpenseControllerTest {

    private static final String VALJAN_ZAHTJEV = """
            {"description":"Namirnice","amount":100.00,"paidBy":1,"category":"FOOD","expenseDate":"2026-11-01",
             "splitType":"EQUAL","participants":[{"userId":1},{"userId":2}]}
            """;

    private final MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ExpenseControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private RequestPostProcessor prijavljen(String idKorisnika) {
        return jwt().jwt(token -> token.subject(idKorisnika));
    }

    private ExpenseResponse odgovor() {
        return new ExpenseResponse(5L, 10L, "Namirnice", new BigDecimal("100.00"),
                new PersonResponse(1L, "Ana"), new PersonResponse(1L, "Ana"), ExpenseCategory.FOOD,
                LocalDate.of(2026, 11, 1), Instant.parse("2026-11-01T12:00:00Z"),
                List.of(new ShareResponse(1L, "Ana", new BigDecimal("50.00")),
                        new ShareResponse(2L, "Emir", new BigDecimal("50.00"))));
    }

    @Test
    void dodavanjeTroskaVraca201() throws Exception {
        when(expenseService.create(eq(1L), eq(10L), any(ExpenseRequest.class))).thenReturn(odgovor());

        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.paidBy.displayName").value("Ana"))
                .andExpect(jsonPath("$.shares.length()").value(2));
    }

    @Test
    void nedostajucaPoljaVracaju400SPoljima() throws Exception {
        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.paidBy").exists())
                .andExpect(jsonPath("$.fieldErrors.expenseDate").exists())
                .andExpect(jsonPath("$.fieldErrors.splitType").exists())
                .andExpect(jsonPath("$.fieldErrors.participants").exists());
    }

    @Test
    void iznosSaTriDecimaleIliNegativanVraca400() throws Exception {
        String triDecimale = VALJAN_ZAHTJEV.replace("100.00", "10.005");
        String negativan = VALJAN_ZAHTJEV.replace("100.00", "-5");

        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content(triDecimale))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content(negativan))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void nepoznatTipPodjeleVraca400() throws Exception {
        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALJAN_ZAHTJEV.replace("EQUAL", "NESTO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void nevaljanaPodjelaIzServisaVraca400SPorukom() throws Exception {
        when(expenseService.create(eq(1L), eq(10L), any(ExpenseRequest.class)))
                .thenThrow(new InvalidSplitException("Zbir dijelova mora biti jednak ukupnom iznosu"));

        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Zbir dijelova mora biti jednak ukupnom iznosu"));
    }

    @Test
    void nekoKoNijeClanGrupeDobija404() throws Exception {
        when(expenseService.create(eq(9L), eq(10L), any(ExpenseRequest.class))).thenThrow(new GroupNotFoundException());

        mockMvc.perform(post("/api/groups/10/expenses").with(prijavljen("9"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isNotFound());
    }

    @Test
    void izmjenaTudegTroskaVraca403() throws Exception {
        when(expenseService.update(eq(2L), eq(10L), eq(5L), any(ExpenseRequest.class)))
                .thenThrow(new NotExpenseAuthorException());

        mockMvc.perform(put("/api/groups/10/expenses/5").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isForbidden());
    }

    @Test
    void izmjenaVracaAzuriranTrosak() throws Exception {
        when(expenseService.update(eq(1L), eq(10L), eq(5L), any(ExpenseRequest.class))).thenReturn(odgovor());

        mockMvc.perform(put("/api/groups/10/expenses/5").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void brisanjeVraca204() throws Exception {
        mockMvc.perform(delete("/api/groups/10/expenses/5").with(prijavljen("1")))
                .andExpect(status().isNoContent());

        verify(expenseService).delete(1L, 10L, 5L);
    }

    @Test
    void brisanjeTudegTroskaVraca403() throws Exception {
        doThrow(new NotExpenseAuthorException()).when(expenseService).delete(2L, 10L, 5L);

        mockMvc.perform(delete("/api/groups/10/expenses/5").with(prijavljen("2")))
                .andExpect(status().isForbidden());
    }

    @Test
    void nepostojeciTrosakVraca404() throws Exception {
        when(expenseService.get(1L, 10L, 99L)).thenThrow(new ExpenseNotFoundException());

        mockMvc.perform(get("/api/groups/10/expenses/99").with(prijavljen("1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listaPrenosiParametreIVracaStranicu() throws Exception {
        when(expenseService.list(1L, 10L, 2, 5, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                ExpenseCategory.FOOD))
                .thenReturn(new PageResponse<>(List.of(odgovor()), 2, 5, 11, 3));

        mockMvc.perform(get("/api/groups/10/expenses").with(prijavljen("1"))
                        .param("page", "2").param("size", "5")
                        .param("from", "2026-10-01").param("to", "2026-10-31").param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalItems").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void lostFormatDatumaUParametruVraca400() throws Exception {
        mockMvc.perform(get("/api/groups/10/expenses").with(prijavljen("1")).param("from", "sutra"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bezTokenaSviEndpointiTroskovaVracaju401() throws Exception {
        mockMvc.perform(get("/api/groups/10/expenses")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/groups/10/expenses/5")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/groups/10/expenses").contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/groups/10/expenses/5").contentType(MediaType.APPLICATION_JSON).content(VALJAN_ZAHTJEV))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/groups/10/expenses/5")).andExpect(status().isUnauthorized());
    }
}
