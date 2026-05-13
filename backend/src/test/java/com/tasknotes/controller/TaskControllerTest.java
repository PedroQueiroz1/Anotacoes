package com.tasknotes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasknotes.dto.StatusUpdateRequest;
import com.tasknotes.dto.TaskRequest;
import com.tasknotes.dto.TaskResponse;
import com.tasknotes.exception.GlobalExceptionHandler;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Priority;
import com.tasknotes.model.TaskStatus;
import com.tasknotes.service.TaskService;
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

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  TaskService  service;

    private TaskResponse sample(long id) {
        return new TaskResponse(
                id, 1L, "Buy milk", null, null,
                Priority.MEDIUM, TaskStatus.TODO,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    void findByCategory_returns200WithList() throws Exception {
        when(service.findByCategory(1L)).thenReturn(List.of(sample(1L), sample(2L)));

        mockMvc.perform(get("/api/categories/1/tasks"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2))
               .andExpect(jsonPath("$[0].title").value("Buy milk"));
    }

    @Test
    void findByCategory_returns404_whenCategoryMissing() throws Exception {
        when(service.findByCategory(99L))
                .thenThrow(new ResourceNotFoundException("Categoria não encontrada: 99"));

        mockMvc.perform(get("/api/categories/99/tasks"))
               .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201WithTask() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(sample(10L));

        mockMvc.perform(post("/api/categories/1/tasks")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new TaskRequest("Buy milk", null, null, Priority.MEDIUM))))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(10))
               .andExpect(jsonPath("$.priority").value("MEDIUM"));
    }

    @Test
    void create_returns400_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/categories/1/tasks")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new TaskRequest("", null, null, Priority.LOW))))
               .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns200WithUpdatedTask() throws Exception {
        TaskResponse updated = new TaskResponse(
                1L, 1L, "Updated", "desc", null,
                Priority.HIGH, TaskStatus.IN_PROGRESS,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/tasks/1")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(
                               new TaskRequest("Updated", "desc", null, Priority.HIGH))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Updated"))
               .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void updateStatus_returns200WithNewStatus() throws Exception {
        TaskResponse done = new TaskResponse(
                1L, 1L, "Task", null, null,
                Priority.MEDIUM, TaskStatus.DONE,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(service.updateStatus(eq(1L), any())).thenReturn(done);

        mockMvc.perform(patch("/api/tasks/1/status")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(new StatusUpdateRequest(TaskStatus.DONE))))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/tasks/1"))
               .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns404_whenNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Tarefa não encontrada: 99"))
                .when(service).delete(99L);

        mockMvc.perform(delete("/api/tasks/99"))
               .andExpect(status().isNotFound());
    }
}
