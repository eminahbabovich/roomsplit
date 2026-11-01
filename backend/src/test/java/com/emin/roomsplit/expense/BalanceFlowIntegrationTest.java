package com.emin.roomsplit.expense;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.group.Group;
import com.emin.roomsplit.group.GroupMember;
import com.emin.roomsplit.group.GroupMemberRepository;
import com.emin.roomsplit.group.GroupRepository;
import com.emin.roomsplit.group.GroupRole;
import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;

import jakarta.persistence.EntityManager;

// Cijeli tok balansa s pravom bazom: SUM/GROUP BY upiti, algoritam i uplate zajedno
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BalanceFlowIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final EntityManager entityManager;

    private User ana;
    private User emir;
    private User sara;
    private User stranac;
    private Group grupa;

    @Autowired
    BalanceFlowIntegrationTest(MockMvc mockMvc, UserRepository userRepository, GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository, EntityManager entityManager) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.entityManager = entityManager;
    }

    @BeforeEach
    void postavi() {
        ana = noviKorisnik("Ana");
        emir = noviKorisnik("Emir");
        sara = noviKorisnik("Sara");
        stranac = noviKorisnik("Stranac");
        grupa = groupRepository.save(new Group("Stan", UUID.randomUUID().toString().substring(0, 8), ana));
        groupMemberRepository.save(new GroupMember(grupa, ana, GroupRole.OWNER));
        groupMemberRepository.save(new GroupMember(grupa, emir, GroupRole.MEMBER));
        groupMemberRepository.save(new GroupMember(grupa, sara, GroupRole.MEMBER));
    }

    private User noviKorisnik(String ime) {
        return userRepository.save(new User(ime + "-" + UUID.randomUUID() + "@example.com", "HASH", ime));
    }

    private RequestPostProcessor kao(User user) {
        return jwt().jwt(token -> token.subject(String.valueOf(user.getId())));
    }

    private String base() {
        return "/api/groups/" + grupa.getId();
    }

    // Isto kao u ExpenseFlowIntegrationTest: svaki HTTP zahtjev u stvarnosti je zasebna transakcija
    private void noviZahtjev() {
        entityManager.flush();
        entityManager.clear();
    }

    // Ana plaća trošak koji se dijeli jednako na troje
    private void anaPlatila(String iznos) throws Exception {
        String json = """
                {"description":"Namirnice","amount":%s,"paidBy":%d,"category":"FOOD","expenseDate":"2026-11-01",
                 "splitType":"EQUAL","participants":[{"userId":%d},{"userId":%d},{"userId":%d}]}
                """.formatted(iznos, ana.getId(), ana.getId(), emir.getId(), sara.getId());
        mockMvc.perform(post(base() + "/expenses").with(kao(ana)).contentType(MediaType.APPLICATION_JSON)
                .content(json)).andExpect(status().isCreated());
        noviZahtjev();
    }

    private void uplati(User platilac, User primalac, String iznos) throws Exception {
        mockMvc.perform(post(base() + "/settlements").with(kao(platilac)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUser\":%d,\"amount\":%s}".formatted(primalac.getId(), iznos)))
                .andExpect(status().isCreated());
        noviZahtjev();
    }

    @Test
    void prazanDnevnikDajeNuleIPrazanPrijedlog() throws Exception {
        mockMvc.perform(get(base() + "/balances").with(kao(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].balance").value(0))
                .andExpect(jsonPath("$[2].balance").value(0));
        mockMvc.perform(get(base() + "/settle-suggestions").with(kao(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void devedesetKMNaTrojeDajeMinus30IPlus60() throws Exception {
        anaPlatila("90.00");

        mockMvc.perform(get(base() + "/balances").with(kao(emir)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.displayName=='Ana')].balance").value(60.0))
                .andExpect(jsonPath("$[?(@.displayName=='Emir')].balance").value(-30.0))
                .andExpect(jsonPath("$[?(@.displayName=='Sara')].balance").value(-30.0));
        mockMvc.perform(get(base() + "/settle-suggestions").with(kao(emir)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].to.displayName").value("Ana"))
                .andExpect(jsonPath("$[0].amount").value(30.0))
                .andExpect(jsonPath("$[1].to.displayName").value("Ana"))
                .andExpect(jsonPath("$[1].amount").value(30.0));
    }

    @Test
    void stoKMNaTrojeNeGubiFeninge() throws Exception {
        anaPlatila("100.00");

        // Ana plati 100.00, a njen dio je 33.34 pa joj duguju 66.66 (33.33 + 33.33)
        mockMvc.perform(get(base() + "/balances").with(kao(ana)))
                .andExpect(jsonPath("$[?(@.displayName=='Ana')].balance").value(66.66))
                .andExpect(jsonPath("$[?(@.displayName=='Emir')].balance").value(-33.33))
                .andExpect(jsonPath("$[?(@.displayName=='Sara')].balance").value(-33.33));
    }

    @Test
    void uplataSmanjujeDugIPrijedlog() throws Exception {
        anaPlatila("90.00");
        uplati(emir, ana, "30.00");

        mockMvc.perform(get(base() + "/balances").with(kao(ana)))
                .andExpect(jsonPath("$[?(@.displayName=='Ana')].balance").value(30.0))
                .andExpect(jsonPath("$[?(@.displayName=='Emir')].balance").value(0.0))
                .andExpect(jsonPath("$[?(@.displayName=='Sara')].balance").value(-30.0));
        mockMvc.perform(get(base() + "/settle-suggestions").with(kao(ana)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].from.displayName").value("Sara"))
                .andExpect(jsonPath("$[0].amount").value(30.0));
    }

    @Test
    void uplataSamomSebiVraca400() throws Exception {
        mockMvc.perform(post(base() + "/settlements").with(kao(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUser\":%d,\"amount\":5.00}".formatted(ana.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uplataNekomeKoNijeClanVraca400() throws Exception {
        mockMvc.perform(post(base() + "/settlements").with(kao(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUser\":%d,\"amount\":5.00}".formatted(stranac.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nijeClanNeVidiBalanseNiPrijedlogNiBiljeziUplatu() throws Exception {
        mockMvc.perform(get(base() + "/balances").with(kao(stranac))).andExpect(status().isNotFound());
        mockMvc.perform(get(base() + "/settle-suggestions").with(kao(stranac))).andExpect(status().isNotFound());
        mockMvc.perform(post(base() + "/settlements").with(kao(stranac)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUser\":%d,\"amount\":5.00}".formatted(ana.getId())))
                .andExpect(status().isNotFound());
    }
}
