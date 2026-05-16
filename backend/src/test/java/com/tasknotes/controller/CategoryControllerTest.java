package com.tasknotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.GlobalExceptionHandler;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.service.CategoryService;
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

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;
    @MockBean  CategoryService service;

    private CategoryResponse sample() {
        return new CategoryResponse(1L, "Work", "work", LocalDateTime.now(), LocalDateTime.now(), 2);
    }

    @Test
    void findAll_returns200WithList() throws Exception {
        when(service.findAll()).thenReturn(List.of(sample()));

        mockMvc.perform(get("/api/categories"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value(1))
               .andExpect(jsonPath("$[0].name").value("Work"))
               .andExpect(jsonPath("$[0].pendingTaskCount").value(2));
    }

    @Test
    void findById_returns200_whenExists() throws Exception {
        when(service.findById(1L)).thenReturn(sample());

        mockMvc.perform(get("/api/categories/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Work"));
    }

    @Test
    void findById_returns404_whenNotFound() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Categoria não encontrada: 99"));

        mockMvc.perform(get("/api/categories/99"))
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_returns201WithCreatedCategory() throws Exception {
        when(service.create(any())).thenReturn(sample());

        mockMvc.perform(post("/api/categories")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new CategoryRequest("Work"))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.name").value("Work"));
    }

    @Test
    void create_returns400_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/categories")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new CategoryRequest(""))))
               .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns422_whenLimitReached() throws Exception {
        when(service.create(any()))
                .thenThrow(new BusinessException("Limite máximo de 5 categorias atingido."));

        mockMvc.perform(post("/api/categories")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new CategoryRequest("Extra"))))
               .andExpect(status().isUnprocessableEntity())
               .andExpect(jsonPath("$.message").value("Limite máximo de 5 categorias atingido."));
    }

    @Test
    void update_returns200WithUpdatedCategory() throws Exception {
        CategoryResponse updated = new CategoryResponse(1L, "Personal", "personal", LocalDateTime.now(), LocalDateTime.now(), 0);
        when(service.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/categories/1")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new CategoryRequest("Personal"))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Personal"));
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/categories/1"))
               .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Categoria não encontrada: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/categories/99"))
               .andExpect(status().isNotFound());
    }
}
