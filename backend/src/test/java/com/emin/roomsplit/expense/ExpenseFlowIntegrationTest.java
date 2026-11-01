package com.emin.roomsplit.expense;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.jayway.jsonpath.JsonPath;

// Cijeli tok s pravom bazom: pravi servis, upiti i ograničenja. Prijavu glumimo jwt() post-procesorom.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExpenseFlowIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    private User ana;
    private User emir;
    private User sara;
    private User stranac;
    private Group grupa;

    @Autowired
    ExpenseFlowIntegrationTest(MockMvc mockMvc, UserRepository userRepository, GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
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

    private String url() {
        return "/api/groups/" + grupa.getId() + "/expenses";
    }

    private String jednakoNaTroje(String opis, String iznos, String kategorija, String datum) {
        return """
                {"description":"%s","amount":%s,"paidBy":%d,"category":"%s","expenseDate":"%s",
                 "splitType":"EQUAL","participants":[{"userId":%d},{"userId":%d},{"userId":%d}]}
                """.formatted(opis, iznos, ana.getId(), kategorija, datum, ana.getId(), emir.getId(), sara.getId());
    }

    private Integer dodaj(User autor, String json) throws Exception {
        String odgovor = mockMvc.perform(post(url()).with(kao(autor))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(odgovor, "$.id");
    }

    @Test
    void stoKMNaTrojeDajeTacnoStoUZbiru() throws Exception {
        mockMvc.perform(post(url()).with(kao(ana)).contentType(MediaType.APPLICATION_JSON)
                        .content(jednakoNaTroje("Namirnice", "100.00", "FOOD", "2026-11-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares[0].amount").value(33.34))
                .andExpect(jsonPath("$.shares[1].amount").value(33.33))
                .andExpect(jsonPath("$.shares[2].amount").value(33.33))
                .andExpect(jsonPath("$.createdBy.userId").value(ana.getId()));
    }

    @Test
    void tacniIznosiSeSpremajuIVracajuPriCitanju() throws Exception {
        String json = """
                {"description":"Večera","amount":50.00,"paidBy":%d,"expenseDate":"2026-11-01","splitType":"EXACT",
                 "participants":[{"userId":%d,"amount":30.50},{"userId":%d,"amount":19.50}]}
                """.formatted(emir.getId(), ana.getId(), emir.getId());
        Integer id = dodaj(ana, json);

        mockMvc.perform(get(url() + "/" + id).with(kao(sara)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("OTHER"))
                .andExpect(jsonPath("$.paidBy.userId").value(emir.getId()))
                .andExpect(jsonPath("$.shares.length()").value(2));
    }

    @Test
    void paginacijaISortiranjePoDatumuRadi() throws Exception {
        dodaj(ana, jednakoNaTroje("Prvi", "10.00", "FOOD", "2026-10-01"));
        dodaj(ana, jednakoNaTroje("Drugi", "20.00", "RENT", "2026-10-15"));
        dodaj(ana, jednakoNaTroje("Treci", "30.00", "FOOD", "2026-11-01"));

        mockMvc.perform(get(url()).with(kao(emir)).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items[0].description").value("Treci"))
                .andExpect(jsonPath("$.items[1].description").value("Drugi"))
                .andExpect(jsonPath("$.items[0].shares.length()").value(3));

        mockMvc.perform(get(url()).with(kao(emir)).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Prvi"));
    }

    @Test
    void filtriranjePoKategorijiIDatumuRadi() throws Exception {
        dodaj(ana, jednakoNaTroje("Prvi", "10.00", "FOOD", "2026-10-01"));
        dodaj(ana, jednakoNaTroje("Drugi", "20.00", "RENT", "2026-10-15"));
        dodaj(ana, jednakoNaTroje("Treci", "30.00", "FOOD", "2026-11-01"));

        mockMvc.perform(get(url()).with(kao(ana)).param("category", "RENT"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Drugi"));

        mockMvc.perform(get(url()).with(kao(ana)).param("from", "2026-10-10").param("to", "2026-10-31"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Drugi"));

        mockMvc.perform(get(url()).with(kao(ana)).param("from", "2026-10-31").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void autorMijenjaTrosakIDijeloviSeZamjenjujuZaIsteKorisnike() throws Exception {
        Integer id = dodaj(ana, jednakoNaTroje("Struja", "90.00", "UTILITIES", "2026-11-01"));
        String izmijenjeno = """
                {"description":"Struja i voda","amount":100.00,"paidBy":%d,"category":"UTILITIES",
                 "expenseDate":"2026-11-02","splitType":"EXACT",
                 "participants":[{"userId":%d,"amount":70.00},{"userId":%d,"amount":30.00}]}
                """.formatted(emir.getId(), ana.getId(), emir.getId());

        mockMvc.perform(put(url() + "/" + id).with(kao(ana)).contentType(MediaType.APPLICATION_JSON).content(izmijenjeno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Struja i voda"))
                .andExpect(jsonPath("$.shares.length()").value(2));

        mockMvc.perform(get(url() + "/" + id).with(kao(ana)))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.paidBy.userId").value(emir.getId()))
                .andExpect(jsonPath("$.shares.length()").value(2));
    }

    @Test
    void mijenjatiIBrisatiMozeSamoAutorAOstaliClanoviDobijaju403() throws Exception {
        Integer id = dodaj(ana, jednakoNaTroje("Struja", "90.00", "UTILITIES", "2026-11-01"));

        mockMvc.perform(put(url() + "/" + id).with(kao(emir)).contentType(MediaType.APPLICATION_JSON)
                        .content(jednakoNaTroje("Hakovano", "1.00", "OTHER", "2026-11-01")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(url() + "/" + id).with(kao(emir)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(url() + "/" + id).with(kao(ana)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(url() + "/" + id).with(kao(ana)))
                .andExpect(status().isNotFound());
    }

    @Test
    void stranacNijeClanPaDobija404NaSveOperacije() throws Exception {
        Integer id = dodaj(ana, jednakoNaTroje("Struja", "90.00", "UTILITIES", "2026-11-01"));

        mockMvc.perform(get(url()).with(kao(stranac))).andExpect(status().isNotFound());
        mockMvc.perform(get(url() + "/" + id).with(kao(stranac))).andExpect(status().isNotFound());
        mockMvc.perform(post(url()).with(kao(stranac)).contentType(MediaType.APPLICATION_JSON)
                        .content(jednakoNaTroje("Hakovano", "1.00", "OTHER", "2026-11-01")))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(url() + "/" + id).with(kao(stranac))).andExpect(status().isNotFound());
    }

    @Test
    void nevaljanaPodjelaSeOdbijaSa400() throws Exception {
        // Platilac nije član grupe
        String tudjiPlatilac = jednakoNaTroje("X", "10.00", "FOOD", "2026-11-01")
                .replace("\"paidBy\":" + ana.getId(), "\"paidBy\":" + stranac.getId());
        mockMvc.perform(post(url()).with(kao(ana)).contentType(MediaType.APPLICATION_JSON).content(tudjiPlatilac))
                .andExpect(status().isBadRequest());

        // Zbir tačnih iznosa ne pogađa ukupan iznos
        String losZbir = """
                {"description":"X","amount":100.00,"paidBy":%d,"expenseDate":"2026-11-01","splitType":"EXACT",
                 "participants":[{"userId":%d,"amount":60.00},{"userId":%d,"amount":39.99}]}
                """.formatted(ana.getId(), ana.getId(), emir.getId());
        mockMvc.perform(post(url()).with(kao(ana)).contentType(MediaType.APPLICATION_JSON).content(losZbir))
                .andExpect(status().isBadRequest());

        // Nijedan neispravan trošak nije ostao u bazi
        mockMvc.perform(get(url()).with(kao(ana)))
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
