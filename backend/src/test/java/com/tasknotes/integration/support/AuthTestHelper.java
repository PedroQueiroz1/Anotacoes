package com.tasknotes.integration.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestHelper {

    private AuthTestHelper() {}

    public static String login(MockMvc mockMvc, ObjectMapper mapper,
                               String username, String password) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "rememberMe", false));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> resp = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) resp.get("accessToken");
    }

    public static String loginWithRememberMe(MockMvc mockMvc, ObjectMapper mapper,
                                             String username, String password) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "username", username,
                "password", password,
                "rememberMe", true));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> resp = mapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return (String) resp.get("accessToken");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
