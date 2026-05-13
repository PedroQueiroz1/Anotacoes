package com.tasknotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.dto.NoteRequest;
import com.tasknotes.dto.NoteResponse;
import com.tasknotes.exception.GlobalExceptionHandler;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
@Import(GlobalExceptionHandler.class)
class NoteControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  NoteService  service;

    private NoteResponse sample(long id) {
        return new NoteResponse(id, 1L, "Meeting notes", "Content here",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getById_returns200_whenExists() throws Exception {
        when(service.findById(1L)).thenReturn(sample(1L));

        mockMvc.perform(get("/api/notes/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.title").value("Meeting notes"));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(service.findById(99L))
                .thenThrow(new ResourceNotFoundException("Anotação não encontrada: 99"));

        mockMvc.perform(get("/api/notes/99"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listByCategory_returns200WithList() throws Exception {
        when(service.findByCategory(1L)).thenReturn(List.of(sample(1L), sample(2L)));

        mockMvc.perform(get("/api/categories/1/notes"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].title").value("Meeting notes"));
    }

    @Test
    void create_returns201WithNote() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(sample(5L));

        mockMvc.perform(post("/api/categories/1/notes")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new NoteRequest("Meeting notes", "Content here"))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(5))
               .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    void create_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/categories/1/notes")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new NoteRequest("", null))))
               .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200WithUpdatedNote() throws Exception {
        NoteResponse updated = new NoteResponse(1L, 1L, "Updated", "New content",
                LocalDateTime.now(), LocalDateTime.now());
        when(service.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/notes/1")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new NoteRequest("Updated", "New content"))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Updated"))
               .andExpect(jsonPath("$.content").value("New content"));
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/notes/1"))
               .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Anotação não encontrada: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/notes/99"))
               .andExpect(status().isNotFound());
    }
}
