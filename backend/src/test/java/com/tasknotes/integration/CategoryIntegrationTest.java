package com.tasknotes.integration;

import com.tasknotes.integration.support.AbstractIntegrationTest;
import com.tasknotes.integration.support.AuthTestHelper;
import com.tasknotes.integration.support.TestDataFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryIntegrationTest extends AbstractIntegrationTest {

    private String adminToken;

    @BeforeAll
    void setUp() throws Exception {
        adminToken = AuthTestHelper.login(mockMvc, objectMapper, ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    @Test
    void createCategory_persistsAndReturnsSlug() throws Exception {
        String userToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "catcreate");

        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userToken, "Minha Categoria Test");

        assertThat(cat.get("id")).isNotNull();
        assertThat(cat.get("slug")).isNotNull();
        assertThat(cat.get("name")).isEqualTo("Minha Categoria Test");
    }

    @Test
    void listCategories_returnsOnlyOwnerCategories() throws Exception {
        String userAToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "catisoA");
        String userBToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "catisoB");

        String uniqueName = "ExclusiveForA_" + TestDataFactory.uniquePrefix();
        TestDataFactory.createCategory(mockMvc, objectMapper, userAToken, uniqueName);

        var result = mockMvc.perform(get("/api/categories")
                        .header("Authorization", AuthTestHelper.bearer(userBToken)))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(uniqueName);
    }

    @Test
    void createSixCategories_returnsError() throws Exception {
        String userToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "catlimit");

        for (int i = 1; i <= 5; i++) {
            TestDataFactory.createCategory(mockMvc, objectMapper, userToken, "Categoria " + i + " " + TestDataFactory.uniquePrefix());
        }

        var sixthName = "Sexta categoria";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/categories")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header("Authorization", AuthTestHelper.bearer(userToken))
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", sixthName))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getCategory_bySlug_returnsCorrectData() throws Exception {
        String userToken = TestDataFactory.createUserAndLogin(mockMvc, objectMapper, adminToken, "catslug");
        var cat = TestDataFactory.createCategory(mockMvc, objectMapper, userToken, "SlugTest " + TestDataFactory.uniquePrefix());
        String slug = TestDataFactory.slugOf(cat);

        mockMvc.perform(get("/api/categories/slug/" + slug)
                        .header("Authorization", AuthTestHelper.bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));
    }
}
