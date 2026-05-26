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
class SearchIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void searchByTaskTitle_findsTask() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchtitle");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        String uniqueTitle = "ProgramacaoJava_" + TestDataFactory.uniquePrefix();
        TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), uniqueTitle);

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", uniqueTitle)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(uniqueTitle);
        assertThat(body).contains("TASK");
    }

    @Test
    void searchByTaskDescription_findsTask() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchdesc");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        String uid = TestDataFactory.uniquePrefix();
        String uniqueDesc = "backendModularArch_" + uid;

        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat), "Task " + uid);
        long taskId = TestDataFactory.idOf(task);

        // Update task with description
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Task " + uid);
        body.put("description", uniqueDesc);
        body.put("priority", "LOW");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/tarefas/" + taskId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", uniqueDesc)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("TASK");
    }

    @Test
    void searchByNoteContent_findsNote() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchnote");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        String uniqueTerm = "HexagonalArchitecture_" + TestDataFactory.uniquePrefix();
        TestDataFactory.createNote(mockMvc, objectMapper, token, TestDataFactory.idOf(cat),
                "Anotação Arch", uniqueTerm);

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", uniqueTerm)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("NOTE");
    }

    @Test
    void searchBySubtaskText_findsParentTask() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchsub");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat),
                "Assistir videos tecnicos " + TestDataFactory.uniquePrefix());
        String uniqueSubText = "GraphQLvsREST_" + TestDataFactory.uniquePrefix();
        TestDataFactory.createSubtask(mockMvc, objectMapper, token, TestDataFactory.idOf(task), uniqueSubText);

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", uniqueSubText)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("TASK");
    }

    @Test
    void searchByYouTubeResolvedTitle_findsParentTask() throws Exception {
        // Simulates a subtask whose text is the resolved YouTube title
        // (the system stores the display title in the `text` field after resolution)
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchyt");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        var task = TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat),
                "Playlist de Estudos");
        String resolvedTitle = "Curso de Programacao Java para Iniciantes " + TestDataFactory.uniquePrefix();
        TestDataFactory.createSubtask(mockMvc, objectMapper, token, TestDataFactory.idOf(task), resolvedTitle);

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", "Programacao Java")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("TASK");
    }

    @Test
    void search_caseInsensitive() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchcase");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        String uid = TestDataFactory.uniquePrefix();
        TestDataFactory.createTask(mockMvc, objectMapper, token, TestDataFactory.idOf(cat),
                "Arquitetura Modular " + uid);

        var resultLower = mockMvc.perform(get("/api/busca")
                        .param("query", "arquitetura modular " + uid)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        var resultUpper = mockMvc.perform(get("/api/busca")
                        .param("query", "ARQUITETURA MODULAR " + uid)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(resultLower.getResponse().getContentAsString()).contains("TASK");
        assertThat(resultUpper.getResponse().getContentAsString()).contains("TASK");
    }

    @Test
    void search_isolatedByUser() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchisoA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "srchisoB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat " + TestDataFactory.uniquePrefix());
        String uniqueTitle = "SegredoDoUserA_" + TestDataFactory.uniquePrefix();
        TestDataFactory.createTask(mockMvc, objectMapper, userAToken, TestDataFactory.idOf(cat), uniqueTitle);

        var result = mockMvc.perform(get("/api/busca")
                        .param("query", uniqueTitle)
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain(uniqueTitle);
    }
}
