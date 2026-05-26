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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void exportCategory_containsCategoryTasksSubtasksAndNotes() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "export");
        String uid = TestDataFactory.uniquePrefix();
        String catName = "Exportar Cat " + uid;
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, catName);
        long catId = TestDataFactory.idOf(cat);
        String slug = TestDataFactory.slugOf(cat);

        String taskTitle = "Tarefa Exportada " + uid;
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, catId, taskTitle);
        long taskId = TestDataFactory.idOf(task);

        String subtaskText = "Subtarefa exportada " + uid;
        TestDataFactory.createSubtask(mockMvc, objectMapper, token, taskId, subtaskText);

        String noteTitle = "Anotação Exportada " + uid;
        String noteContent = "Conteúdo exportado " + uid;
        TestDataFactory.createNote(mockMvc, objectMapper, token, catId, noteTitle, noteContent);

        var result = mockMvc.perform(get("/api/categorias/" + slug + "/export/txt")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains(catName);
        assertThat(content).contains(taskTitle);
        assertThat(content).contains(subtaskText);
        assertThat(content).contains(noteTitle);
        assertThat(content).contains(noteContent);
        assertThat(content).contains("TAREFAS");
        assertThat(content).contains("ANOTAÇÕES");
    }

    @Test
    void userB_cannotExportUserACategory() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "exportisoA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "exportisoB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken,
                "Cat Privada " + TestDataFactory.uniquePrefix());
        String slug = TestDataFactory.slugOf(cat);

        mockMvc.perform(get("/api/categorias/" + slug + "/export/txt")
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());
    }
}
