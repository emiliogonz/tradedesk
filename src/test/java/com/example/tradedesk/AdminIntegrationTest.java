package com.example.tradedesk;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTestBase {

    @Test
    void traderCannotChangeRoles() throws Exception {
        User target = createUser(Role.TRADER);
        String token = loginAs(createUser(Role.TRADER));

        changeRole(token, target.getId(), "RISK")
                .andExpect(status().isForbidden());

        assertThat(roleOf(target)).isEqualTo(Role.TRADER);
    }

    @Test
    void adminCanPromoteTraderToRisk() throws Exception {
        User target = createUser(Role.TRADER);
        String token = loginAs(createUser(Role.ADMIN));

        changeRole(token, target.getId(), "RISK")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RISK"));

        assertThat(roleOf(target)).isEqualTo(Role.RISK);
    }

    @Test
    void unknownRoleIsRejected() throws Exception {
        User target = createUser(Role.TRADER);
        String token = loginAs(createUser(Role.ADMIN));

        changeRole(token, target.getId(), "SUPERUSER")
                .andExpect(status().isBadRequest());

        assertThat(roleOf(target)).isEqualTo(Role.TRADER);
    }

    @Test
    void adminCanDemoteAnotherAdminWhenOthersRemain() throws Exception {
        User otherAdmin = createUser(Role.ADMIN);
        String token = loginAs(createUser(Role.ADMIN));

        changeRole(token, otherAdmin.getId(), "TRADER")
                .andExpect(status().isOk());

        assertThat(roleOf(otherAdmin)).isEqualTo(Role.TRADER);
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        User soleAdmin = createUser(Role.ADMIN);
        String token = loginAs(soleAdmin);
        demoteAllAdminsExcept(soleAdmin.getId());

        changeRole(token, soleAdmin.getId(), "TRADER")
                .andExpect(status().isConflict());

        assertThat(roleOf(soleAdmin)).isEqualTo(Role.ADMIN);
    }

    // --- helpers specific to this class ---

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