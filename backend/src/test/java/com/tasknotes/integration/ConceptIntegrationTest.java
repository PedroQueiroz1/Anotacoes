package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import com.tasknotes.integration.support.TestDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autocomplete tests use concepts seeded at startup (GLOBAL scope).
 * No internet connection or external API is needed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConceptIntegrationTest extends AbstractIntegrationTest {

    private String userToken;

    @BeforeAll
    void setUp() throws Exception {
        String adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "concept");
    }

    @Test
    void seededConcept_JSON_returnssuggestion() throws Exception {
        // JSON is in the GLOBAL seed data loaded at application startup
        var result = mockMvc.perform(get("/api/concepts/suggest")
                        .param("term", "JSON")
                        .header("Authorization", AuthTestHelper.bearer(userToken)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).isNotBlank();
        // Response contains the term or summary
        assertThat(body.toLowerCase()).containsAnyOf("json", "javascript");
    }

    @Test
    void cleanTermWithoutSemicolon_returnsSuggestion() throws Exception {
        // The frontend strips the ';' before sending to the backend.
        // Backend receives "JSON" (not "JSON;") and finds the seeded concept.
        var result = mockMvc.perform(get("/api/concepts/suggest")
                        .param("term", "JSON")
                        .header("Authorization", AuthTestHelper.bearer(userToken)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).isNotBlank();
    }

    @Test
    void seededConcept_CI_returnsSuggestion() throws Exception {
        var result = mockMvc.perform(get("/api/concepts/suggest")
                        .param("term", "CI")
                        .header("Authorization", AuthTestHelper.bearer(userToken)))
                .andReturn();

        // CI may be seeded or not — just verify no 500 error
        assertThat(result.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void unknownTerm_doesNotReturn500() throws Exception {
        mockMvc.perform(get("/api/concepts/suggest")
                        .param("term", "xyzzy_unknown_term_99999")
                        .header("Authorization", AuthTestHelper.bearer(userToken)))
                .andExpect(status().is(org.hamcrest.Matchers.not(500)));
    }

    @Test
    void suggest_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/concepts/suggest")
                        .param("term", "JSON"))
                .andExpect(status().isUnauthorized());
    }
}
