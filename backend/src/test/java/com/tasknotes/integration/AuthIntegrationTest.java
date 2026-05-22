package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginValidCredentials_returns200WithToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD,
                "rememberMe", false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void loginInvalidPassword_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", ADMIN_USERNAME,
                "password", "wrongpassword",
                "rememberMe", false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidToken_returns200() throws Exception {
        String token = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshToken_withRememberMe_returnsNewAccessToken() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD,
                "rememberMe", true));

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        var cookies = loginResult.getResponse().getCookies();
        var refreshCookie = java.util.Arrays.stream(cookies)
                .filter(c -> c.getName().equals("refreshToken"))
                .findFirst();

        if (refreshCookie.isPresent()) {
            mockMvc.perform(post("/api/auth/refresh")
                            .cookie(refreshCookie.get()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    @Test
    void logout_returns204() throws Exception {
        String token = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isNoContent());
    }
}
