package com.emin.roomsplit.auth;

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
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

// Cijeli tok s pravom bazom: registracija -> prijava -> zaštićen endpoint s tokenom.
// Traži Docker bazu i učitane varijable iz .env; JWT ključ dolazi iz profila "test".
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    AuthFlowIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private String registrujSaEmailom(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"tajnaLozinka1","displayName":"Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        return email;
    }

    @Test
    void registracijaPrijavaIZasticenEndpointRadeZajedno() throws Exception {
        String email = registrujSaEmailom("flow-" + UUID.randomUUID() + "@example.com");

        String odgovor = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"tajnaLozinka1"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(odgovor, "$.accessToken");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void prijavaSPogresnomLozinkomVraca401() throws Exception {
        String email = registrujSaEmailom("flow-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"pogresnaLozinka"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void zasticenEndpointBezTokenaIsLosimTokenomVraca401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer ovo.nije.token"))
                .andExpect(status().isUnauthorized());
    }
}
