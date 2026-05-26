package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import com.tasknotes.integration.support.TestDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaginationIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void taskPagination_firstPageHas10Items_hasNextTrue() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "pagtask");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        for (int i = 1; i <= 12; i++) {
            TestDataFactory.createTask(mockMvc, objectMapper, token, catId, "Tarefa " + i + " " + TestDataFactory.uniquePrefix());
        }

        var result = mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items = (List<Map<?, ?>>) page.get("items");

        assertThat(items).hasSize(10);
        assertThat((Boolean) page.get("hasNext")).isTrue();
        assertThat(page.get("nextCursor")).isNotNull();
    }

    @Test
    void taskPagination_secondPageWithCursor_returnsRemaining() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "pagtask2");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        for (int i = 1; i <= 12; i++) {
            TestDataFactory.createTask(mockMvc, objectMapper, token, catId, "Task " + i + " " + TestDataFactory.uniquePrefix());
        }

        // Get first page
        var page1Result = mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page1 = objectMapper.readValue(page1Result.getResponse().getContentAsString(), Map.class);
        String cursor = (String) page1.get("nextCursor");
        assertThat(cursor).isNotBlank();

        // Get second page
        var page2Result = mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .param("cursor", cursor)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page2 = objectMapper.readValue(page2Result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items2 = (List<Map<?, ?>>) page2.get("items");

        assertThat(items2).hasSize(2);
        assertThat((Boolean) page2.get("hasNext")).isFalse();
    }

    @Test
    void taskPagination_doesNotDuplicateOrSkipItems() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "paguniq");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        for (int i = 1; i <= 12; i++) {
            TestDataFactory.createTask(mockMvc, objectMapper, token, catId, "UniqueTask " + i + " " + TestDataFactory.uniquePrefix());
        }

        // Collect IDs from both pages
        var page1Result = mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andReturn();
        Map<?, ?> page1 = objectMapper.readValue(page1Result.getResponse().getContentAsString(), Map.class);
        String cursor = (String) page1.get("nextCursor");

        var page2Result = mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .param("cursor", cursor)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andReturn();
        Map<?, ?> page2 = objectMapper.readValue(page2Result.getResponse().getContentAsString(), Map.class);

        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items1 = (List<Map<?, ?>>) page1.get("items");
        @SuppressWarnings("unchecked")
        List<Map<?, ?>> items2 = (List<Map<?, ?>>) page2.get("items");

        Set<Object> allIds = new HashSet<>();
        items1.forEach(t -> allIds.add(t.get("id")));
        items2.forEach(t -> allIds.add(t.get("id")));

        assertThat(allIds).hasSize(12); // no duplicates, no skips
    }

    @Test
    void notePagination_totalCountShowsAllNotes() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "pagnote");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        for (int i = 1; i <= 12; i++) {
            TestDataFactory.createNote(mockMvc, objectMapper, token, catId, "Nota " + i, "Conteúdo " + i);
        }

        var result = mockMvc.perform(get("/api/categorias/" + catId + "/notes")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) page.get("items");

        assertThat(((Number) page.get("totalCount")).longValue()).isEqualTo(12);
        assertThat(items).hasSize(10);
        assertThat((Boolean) page.get("hasNext")).isTrue();
    }

    @Test
    void subtaskList_returnsAllItemsWithinLimit() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "pagsub");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task Subtasks");
        long taskId = TestDataFactory.idOf(task);

        // Create 15 subtasks (well within the 20-per-task limit)
        for (int i = 1; i <= 15; i++) {
            TestDataFactory.createSubtask(mockMvc, objectMapper, token, taskId, "Sub " + i);
        }

        var result = mockMvc.perform(get("/api/tarefas/" + taskId + "/subtasks")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> page = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        List<?> items = (List<?>) page.get("items");

        assertThat(items).hasSize(15);
        assertThat(((Number) page.get("totalCount")).longValue()).isEqualTo(15);
    }

    @Test
    void ownershipRead_userBCannotReadUserACategory() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "ownreadA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "ownreadB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat A " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        // User B cannot list tasks in user A's category
        mockMvc.perform(get("/api/categorias/" + catId + "/tasks")
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());

        // User B cannot list notes in user A's category
        mockMvc.perform(get("/api/categorias/" + catId + "/notes")
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipWrite_userBCannotAddTaskToUserACategory() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "ownwriteA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "ownwriteB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat A " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        // User B cannot create a task in user A's category
        mockMvc.perform(post("/api/categorias/" + catId + "/tasks")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userBToken))
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "title", "Tarefa invasora", "priority", "LOW"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownershipWrite_userBCannotDeleteUserATask() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "owndel1A");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "owndel1B");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat A " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, userAToken, TestDataFactory.idOf(cat), "Tarefa de A");
        long taskId = TestDataFactory.idOf(task);

        mockMvc.perform(delete("/api/tarefas/" + taskId)
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());
    }
}
