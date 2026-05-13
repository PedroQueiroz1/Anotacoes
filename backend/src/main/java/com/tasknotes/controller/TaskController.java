package com.tasknotes.controller;

import com.tasknotes.dto.StatusUpdateRequest;
import com.tasknotes.dto.TaskRequest;
import com.tasknotes.dto.TaskResponse;
import com.tasknotes.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Tarefas", description = "Gerenciamento de tarefas por categoria")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/api/categories/{categoryId}/tasks")
    @Operation(summary = "Listar tarefas de uma categoria (ordenadas por prioridade)")
    public List<TaskResponse> findByCategory(@PathVariable Long categoryId) {
        return service.findByCategory(categoryId);
    }

    @PostMapping("/api/categories/{categoryId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar tarefa em uma categoria")
    public TaskResponse create(
            @PathVariable Long categoryId,
            @Valid @RequestBody TaskRequest request) {
        return service.create(categoryId, request);
    }

    @PutMapping("/api/tasks/{id}")
    @Operation(summary = "Atualizar dados de uma tarefa")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/api/tasks/{id}/status")
    @Operation(summary = "Atualizar status de uma tarefa")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir uma tarefa e suas subtarefas")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
