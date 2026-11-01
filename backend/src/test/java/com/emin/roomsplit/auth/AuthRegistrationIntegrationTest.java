package com.emin.roomsplit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;

// Radi s pravom bazom (Docker Postgres) i traži učitane varijable iz .env.
// @Transactional vraća bazu u prvobitno stanje poslije svakog testa.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRegistrationIntegrationTest {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    AuthRegistrationIntegrationTest(MockMvc mockMvc, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void registracijaUpisujeKorisnikaSaBcryptHashomUBazu() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"tajnaLozinka1","displayName":"Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        User korisnik = userRepository.findByEmail(email).orElseThrow();
        assertThat(korisnik.getPasswordHash()).isNotEqualTo("tajnaLozinka1");
        assertThat(passwordEncoder.matches("tajnaLozinka1", korisnik.getPasswordHash())).isTrue();
    }

    @Test
    void drugaRegistracijaSIstimEmailomVraca409() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String body = """
                {"email":"%s","password":"tajnaLozinka1","displayName":"Test"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }
}
