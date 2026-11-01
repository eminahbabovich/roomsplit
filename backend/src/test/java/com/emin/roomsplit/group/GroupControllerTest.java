package com.emin.roomsplit.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@WebMvcTest(GroupController.class)
@Import(SecurityConfig.class)
class GroupControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    GroupControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private RequestPostProcessor prijavljen(String idKorisnika) {
        return jwt().jwt(token -> token.subject(idKorisnika));
    }

    private GroupDetailsResponse stan() {
        return new GroupDetailsResponse(10L, "Stan", "ABCD2345", Instant.parse("2026-11-01T12:00:00Z"),
                List.of(new MemberResponse(1L, "Ana", GroupRole.OWNER, Instant.parse("2026-11-01T12:00:00Z"))));
    }

    @Test
    void kreiranjeGrupeVraca201IUzimaIdIzTokena() throws Exception {
        when(groupService.create(eq(1L), any(CreateGroupRequest.class))).thenReturn(stan());

        mockMvc.perform(post("/api/groups").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joinCode").value("ABCD2345"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[0].email").doesNotExist());
    }

    @Test
    void kreiranjeBezNazivaVraca400() throws Exception {
        mockMvc.perform(post("/api/groups").with(prijavljen("1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void pridruzivanjeVraca200() throws Exception {
        when(groupService.join(eq(2L), any(JoinGroupRequest.class))).thenReturn(stan());

        mockMvc.perform(post("/api/groups/join").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"ABCD2345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Stan"));
    }

    @Test
    void pridruzivanjeSNepostojecimKodomVraca404() throws Exception {
        when(groupService.join(eq(2L), any(JoinGroupRequest.class))).thenThrow(new GroupNotFoundException());

        mockMvc.perform(post("/api/groups/join").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"NEMA2222\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pridruzivanjeKadSiVecClanVraca409() throws Exception {
        when(groupService.join(eq(2L), any(JoinGroupRequest.class))).thenThrow(new AlreadyGroupMemberException());

        mockMvc.perform(post("/api/groups/join").with(prijavljen("2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"joinCode\":\"ABCD2345\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void mojeGrupeVracaListu() throws Exception {
        when(groupService.myGroups(1L)).thenReturn(List.of(new GroupSummaryResponse(10L, "Stan", GroupRole.OWNER)));

        mockMvc.perform(get("/api/groups").with(prijavljen("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Stan"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void detaljiGrupeKojaNijeTvojaVraca404() throws Exception {
        when(groupService.get(3L, 10L)).thenThrow(new GroupNotFoundException());

        mockMvc.perform(get("/api/groups/10").with(prijavljen("3")))
                .andExpect(status().isNotFound());
    }

    @Test
    void bezTokenaSviEndpointiGrupaVracaju401() throws Exception {
        mockMvc.perform(get("/api/groups")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/groups/10")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/groups").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Stan\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/groups/join").contentType(MediaType.APPLICATION_JSON).content("{\"joinCode\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }
}
