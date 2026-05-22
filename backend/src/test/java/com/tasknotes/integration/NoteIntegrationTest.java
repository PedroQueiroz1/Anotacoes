package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import com.tasknotes.integration.support.TestDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NoteIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void createNote_persistsTitleAndContent() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "notecreate");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        var note = TestDataFactory.createNote(mockMvc, objectMapper, token, catId,
                "Minha Anotação", "Conteúdo da anotação");

        assertThat(note.get("title")).isEqualTo("Minha Anotação");
        assertThat(note.get("content")).isEqualTo("Conteúdo da anotação");
    }

    @Test
    void lineBreaks_arePreservedOnRefetch() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "notebreak");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        String multiline = "Linha 1\nLinha 2\nLinha 3";
        TestDataFactory.createNote(mockMvc, objectMapper, token, catId, "Anotação Multiline", multiline);

        var listResult = mockMvc.perform(get("/api/categories/" + catId + "/notes")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(listResult.getResponse().getContentAsString()).contains("Linha 1\\nLinha 2");
    }

    @Test
    void editNote_persistsChanges() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "noteedit");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        var note = TestDataFactory.createNote(mockMvc, objectMapper, token, catId, "Original", "Conteúdo original");
        long noteId = TestDataFactory.idOf(note);

        mockMvc.perform(put("/api/notes/" + noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(token))
                        .content(objectMapper.writeValueAsString(
                                Map.of("title", "Editado", "content", "Conteúdo editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Editado"))
                .andExpect(jsonPath("$.content").value("Conteúdo editado"));
    }

    @Test
    void deleteNote_returns204NotError500() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "notedel");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        var note = TestDataFactory.createNote(mockMvc, objectMapper, token, catId, "Para Deletar", "x");
        long noteId = TestDataFactory.idOf(note);

        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void totalCount_independentOfCurrentPage() throws Exception {
        String token = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "notecount");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, token, "Cat " + TestDataFactory.uniquePrefix());
        long catId = TestDataFactory.idOf(cat);

        // Create 11 notes (page size is 10)
        for (int i = 1; i <= 11; i++) {
            TestDataFactory.createNote(mockMvc, objectMapper, token, catId, "Nota " + i, "Conteúdo " + i);
        }

        var result = mockMvc.perform(get("/api/categories/" + catId + "/notes")
                        .header("Authorization", AuthTestHelper.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();

        var page = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        long totalCount = ((Number) page.get("totalCount")).longValue();
        int itemsOnPage = ((java.util.List<?>) page.get("items")).size();

        assertThat(totalCount).isEqualTo(11);
        assertThat(itemsOnPage).isEqualTo(10);
        assertThat((Boolean) page.get("hasNext")).isTrue();
    }

    @Test
    void userB_cannotDeleteUserANote() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "noteisoA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "noteisoB");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, "Cat A " + TestDataFactory.uniquePrefix());
        var note = TestDataFactory.createNote(mockMvc, objectMapper, userAToken, TestDataFactory.idOf(cat),
                "Nota de A", "privada");
        long noteId = TestDataFactory.idOf(note);

        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isNotFound());
    }
}
