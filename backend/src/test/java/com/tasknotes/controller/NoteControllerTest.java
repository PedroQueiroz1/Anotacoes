package com.tasknotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.notes.dto.NoteRequest;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.shared.exception.GlobalExceptionHandler;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.notes.service.NoteService;
import com.tasknotes.notes.service.NoteTagService;
import com.tasknotes.notes.controller.NoteController;
import com.tasknotes.shared.config.SecurityConfig;
import com.tasknotes.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = NoteController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
})
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class NoteControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  NoteService  service;
    @MockBean  NoteTagService tagService;

    private NoteResponse sample(long id) {
        return new NoteResponse(id, null, 1L, "Meeting notes", "Content here", null,
                false, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listByCategory_returns200WithList() throws Exception {
        CursorPageResponse<NoteResponse> page = new CursorPageResponse<>(
                List.of(sample(1L), sample(2L)), null, false, 10, null);
        when(service.findByCategory(eq(1L), isNull(), isNull(), anyString(), isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/categorias/1/notes"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.items.length()").value(2))
               .andExpect(jsonPath("$.items[0].title").value("Meeting notes"))
               .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void create_returns201WithNote() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(sample(5L));

        mockMvc.perform(post("/api/categorias/1/notes")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new NoteRequest("Meeting notes", "Content here", null))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(5))
               .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    void create_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/categorias/1/notes")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new NoteRequest("", null, null))))
               .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200WithUpdatedNote() throws Exception {
        NoteResponse updated = new NoteResponse(1L, null, 1L, "Updated", "New content", null,
                false, null, List.of(), LocalDateTime.now(), LocalDateTime.now());
        when(service.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/anotacoes/1")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new NoteRequest("Updated", "New content", null))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Updated"))
               .andExpect(jsonPath("$.content").value("New content"));
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/anotacoes/1"))
               .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Anotação não encontrada: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/anotacoes/99"))
               .andExpect(status().isNotFound());
    }
}
