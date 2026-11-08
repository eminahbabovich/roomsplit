package com.emin.roomsplit.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.emin.roomsplit.config.SecurityConfig;

// Učitavamo pravi SecurityConfig, da test dokaže pravilo iz njega (a ne default Spring Security)
@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    private final MockMvc mockMvc;

    // SecurityConfig traži JwtDecoder; za ovaj test je dovoljan lažni
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    HealthControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    // Dokazuje da health check radi bez tokena (hosting ga zove anonimno)
    @Test
    void healthJeJavanBezPrijave() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
