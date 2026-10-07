package com.example.tradedesk;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void tradesEndpointRejectsRequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/trades"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerCreatesNewUser() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"newuser@tradedesk.dev","password":"testpassword123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@tradedesk.dev"))
                .andExpect(jsonPath("$.role").value("TRADER"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String payload = """
                {"email":"duplicate@tradedesk.dev","password":"testpassword123"}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTokenForCorrectCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"logintest@tradedesk.dev","password":"testpassword123"}
                        """));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"logintest@tradedesk.dev","password":"testpassword123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"wrongpass@tradedesk.dev","password":"testpassword123"}
                        """));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpass@tradedesk.dev","password":"notthepassword"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@nowhere.com","password":"anything"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}