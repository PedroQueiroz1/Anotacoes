package com.tasknotes.integration.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static String uniquePrefix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** Creates a user via the admin endpoint and logs in, returning the access token. */
    public static String createUserAndLogin(MockMvc mockMvc, ObjectMapper mapper,
                                            String adminToken, String prefix) throws Exception {
        String username = prefix + uniquePrefix();
        String email = username + "@test.tasknotes";
        createUser(mockMvc, mapper, adminToken, username, email,
                AbstractIntegrationTest.TEST_USER_PASSWORD, "User " + prefix);
        return AuthTestHelper.login(mockMvc, mapper, username, AbstractIntegrationTest.TEST_USER_PASSWORD);
    }

    public static Map<?, ?> createUser(MockMvc mockMvc, ObjectMapper mapper, String adminToken,
                                       String username, String email, String password,
                                       String displayName) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("displayName", displayName);
        body.put("enabled", true);

        MvcResult result = mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(adminToken))
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    public static Map<?, ?> createCategory(MockMvc mockMvc, ObjectMapper mapper,
                                           String userToken, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userToken))
                        .content(mapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    public static Map<?, ?> createTask(MockMvc mockMvc, ObjectMapper mapper,
                                       String userToken, long categoryId,
                                       String title) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("description", "");
        body.put("priority", "LOW");

        MvcResult result = mockMvc.perform(post("/api/categories/" + categoryId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userToken))
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    public static Map<?, ?> createSubtask(MockMvc mockMvc, ObjectMapper mapper,
                                          String userToken, long taskId, String text) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks/" + taskId + "/subtasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userToken))
                        .content(mapper.writeValueAsString(Map.of("text", text))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    public static Map<?, ?> createNote(MockMvc mockMvc, ObjectMapper mapper,
                                       String userToken, long categoryId,
                                       String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/categories/" + categoryId + "/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userToken))
                        .content(mapper.writeValueAsString(Map.of("title", title, "content", content))))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readValue(result.getResponse().getContentAsString(StandardCharsets.UTF_8), Map.class);
    }

    public static long idOf(Map<?, ?> response) {
        return ((Number) response.get("id")).longValue();
    }

    public static String slugOf(Map<?, ?> response) {
        return (String) response.get("slug");
    }
}
