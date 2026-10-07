package com.example.tradedesk;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.User;
import com.example.tradedesk.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AdminIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String PASSWORD = "testpassword123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void traderCannotChangeRoles() throws Exception {
        User target = createUser("target1@tradedesk.dev", Role.TRADER);
        String token = loginAs(createUser("trader1@tradedesk.dev", Role.TRADER));

        changeRole(token, target.getId(), "RISK")
                .andExpect(status().isForbidden());

        assertThat(roleOf(target)).isEqualTo(Role.TRADER);
    }

    @Test
    void adminCanPromoteTraderToRisk() throws Exception {
        User target = createUser("target2@tradedesk.dev", Role.TRADER);
        String token = loginAs(createUser("admin2@tradedesk.dev", Role.ADMIN));

        changeRole(token, target.getId(), "RISK")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RISK"));

        assertThat(roleOf(target)).isEqualTo(Role.RISK);
    }

    @Test
    void unknownRoleIsRejected() throws Exception {
        User target = createUser("target3@tradedesk.dev", Role.TRADER);
        String token = loginAs(createUser("admin3@tradedesk.dev", Role.ADMIN));

        changeRole(token, target.getId(), "SUPERUSER")
                .andExpect(status().isBadRequest());

        assertThat(roleOf(target)).isEqualTo(Role.TRADER);
    }

    @Test
    void adminCanDemoteAnotherAdminWhenOthersRemain() throws Exception {
        User otherAdmin = createUser("admin4b@tradedesk.dev", Role.ADMIN);
        String token = loginAs(createUser("admin4a@tradedesk.dev", Role.ADMIN));

        changeRole(token, otherAdmin.getId(), "TRADER")
                .andExpect(status().isOk());

        assertThat(roleOf(otherAdmin)).isEqualTo(Role.TRADER);
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        User soleAdmin = createUser("soleadmin@tradedesk.dev", Role.ADMIN);
        String token = loginAs(soleAdmin);
        demoteAllAdminsExcept(soleAdmin.getId());

        changeRole(token, soleAdmin.getId(), "TRADER")
                .andExpect(status().isConflict());

        assertThat(roleOf(soleAdmin)).isEqualTo(Role.ADMIN);
    }

    // --- helpers ---

    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        return userRepository.save(user);
    }

    private String loginAs(User user) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions changeRole(String token, Long userId, String role) throws Exception {
        return mockMvc.perform(put("/api/admin/users/{id}/role", userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"" + role + "\"}"));
    }

    private Role roleOf(User user) {
        return userRepository.findById(user.getId()).orElseThrow().getRole();
    }

    private void demoteAllAdminsExcept(Long keepId) {
        userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN && !u.getId().equals(keepId))
                .forEach(u -> {
                    u.setRole(Role.TRADER);
                    userRepository.save(u);
                });
    }
}