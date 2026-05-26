package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import com.tasknotes.integration.support.TestDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaskIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void createTask_persistsWithDefaultTodoStatus() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "taskcreate");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, catId, "Tarefa Nova");
        assertThat(task.get("status")).isEqualTo("TODO");
        assertThat(task.get("title")).isEqualTo("Tarefa Nova");
    }

    @Test
    void updateStatus_toInProgress_persistsAfterRefetch() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "statusip");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Status IP");
        long taskId = TestDataFactory.idOf(task);

        mockMvc.perform(patch("/api/tarefas/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Refetch via list and verify persistence
        var listResult = mockMvc.perform(get("/api/categorias/" + TestDataFactory.idOf(cat) + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var items = (List<Map<?, ?>>) objectMapper.readValue(
                listResult.getResponse().getContentAsString(), Map.class).get("items");
        assertThat(items).anySatisfy(t -> assertThat(t.get("status")).isEqualTo("IN_PROGRESS"));
    }

    @Test
    void updateStatus_toDone_persistsAfterRefetch() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "statusdone");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Status Done");
        long taskId = TestDataFactory.idOf(task);

        mockMvc.perform(patch("/api/tarefas/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        var listResult = mockMvc.perform(get("/api/categorias/" + TestDataFactory.idOf(cat) + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        var items = (List<Map<?, ?>>) objectMapper.readValue(
                listResult.getResponse().getContentAsString(), Map.class).get("items");
        assertThat(items).anySatisfy(t -> assertThat(t.get("status")).isEqualTo("DONE"));
    }

    @Test
    void partialUpdate_doesNotResetStatus() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "statusreset");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Reset Check");
        long taskId = TestDataFactory.idOf(task);

        // Mark as DONE
        mockMvc.perform(patch("/api/tarefas/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
                .andExpect(status().isOk());

        // Update description only (PUT /api/tarefas/{id})
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("title", "Task Reset Check");
        updateBody.put("description", "Descrição atualizada");
        updateBody.put("priority", "HIGH");

        var updateResult = mockMvc.perform(put("/api/tarefas/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andReturn();

        var updated = objectMapper.readValue(updateResult.getResponse().getContentAsString(), Map.class);
        assertThat(updated.get("status")).isEqualTo("DONE");
    }

    @Test
    void updatePriority_persists() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "priority");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Priority");
        long taskId = TestDataFactory.idOf(task);

        mockMvc.perform(patch("/api/tarefas/" + taskId + "/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(Map.of("priority", "HIGH"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void createSubtask_appearsInList() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "subtasklist");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Subtasks");
        long taskId = TestDataFactory.idOf(task);

        TestDataFactory.createSubtask(mockMvc, objectMapper, token, taskId, "Subtarefa de teste");

        mockMvc.perform(get("/api/tarefas/" + taskId + "/subtasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].text").value("Subtarefa de teste"))
                .andExpect(jsonPath("$.items[0].done").value(false));
    }

    @Test
    void toggleSubtask_persistsDoneState() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "subtasktoggle");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Toggle");
        long taskId = TestDataFactory.idOf(task);
        var sub = TestDataFactory.createSubtask(mockMvc, objectMapper, token, taskId, "Subtarefa toggle");
        long subId = TestDataFactory.idOf(sub);

        mockMvc.perform(patch("/api/subtarefas/" + subId + "/toggle")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mockMvc.perform(get("/api/tarefas/" + taskId + "/subtasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].done").value(true));
    }

    @Test
    void userB_cannotAccessUserATask() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "taskisoA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "taskisoB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat A " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, userAToken, TestDataFactory.idOf(cat), "Task de A");
        long taskId = TestDataFactory.idOf(task);

        mockMvc.perform(patch("/api/tarefas/" + taskId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userBToken))
                        .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tarefas/" + taskId)
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());
    }
}
