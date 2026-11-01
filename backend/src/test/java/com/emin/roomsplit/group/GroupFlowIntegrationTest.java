package com.emin.roomsplit.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;
import com.jayway.jsonpath.JsonPath;

// Pravi servis i prava baza: kreiranje grupe, pridruživanje i provjera da stranac ne vidi grupu.
// Prijavu glumimo jwt() post-procesorom, a korisnici su pravi redovi u bazi (rollback poslije testa).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupFlowIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;

    @Autowired
    GroupFlowIntegrationTest(MockMvc mockMvc, UserRepository userRepository) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
    }

    private User noviKorisnik(String ime) {
        return userRepository.save(new User(ime + "-" + UUID.randomUUID() + "@example.com", "HASH", ime));
    }

    private RequestPostProcessor kao(User user) {
        return jwt().jwt(token -> token.subject(String.valueOf(user.getId())));
    }

    private String kreirajGrupu(User vlasnik, String naziv) throws Exception {
        return mockMvc.perform(post("/api/groups").with(kao(vlasnik))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + naziv + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void vlasnikKreiraGrupuClanSePridruziAStranacJeNeVidi() throws Exception {
        User ana = noviKorisnik("Ana");
        User emir = noviKorisnik("Emir");
        User stranac = noviKorisnik("Stranac");

        String odgovor = kreirajGrupu(ana, "Stan");
        Integer groupId = JsonPath.read(odgovor, "$.id");
        String kod = JsonPath.read(odgovor, "$.joinCode");
        assertThat(kod).hasSize(JoinCodeGenerator.LENGTH);

        mockMvc.perform(post("/api/groups/join").with(kao(emir))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"" + kod.toLowerCase() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));

        mockMvc.perform(get("/api/groups/" + groupId).with(kao(emir)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Stan"));

        mockMvc.perform(get("/api/groups/" + groupId).with(kao(stranac)))
                .andExpect(status().isNotFound());
    }

    @Test
    void mojeGrupeSadrzeSamoGrupeUKojimaSiClan() throws Exception {
        User ana = noviKorisnik("Ana");
        User emir = noviKorisnik("Emir");
        kreirajGrupu(ana, "Stan");
        kreirajGrupu(emir, "Putovanje");

        mockMvc.perform(get("/api/groups").with(kao(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Stan"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void drugoPridruzivanjeIstojGrupiVraca409() throws Exception {
        User ana = noviKorisnik("Ana");
        User emir = noviKorisnik("Emir");
        String kod = JsonPath.read(kreirajGrupu(ana, "Stan"), "$.joinCode");
        String body = "{\"joinCode\":\"" + kod + "\"}";

        mockMvc.perform(post("/api/groups/join").with(kao(emir)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/groups/join").with(kao(emir)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        // Vlasnik se također ne može "pridružiti" vlastitoj grupi
        mockMvc.perform(post("/api/groups/join").with(kao(ana)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void pridruzivanjeSPogresnimKodomVraca404() throws Exception {
        User emir = noviKorisnik("Emir");

        mockMvc.perform(post("/api/groups/join").with(kao(emir))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"NEMA2222\"}"))
                .andExpect(status().isNotFound());
    }
}
