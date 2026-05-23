package com.tasknotes.tasks.controller;

import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.tasks.dto.StatusUpdateRequest;
import com.tasknotes.tasks.dto.TaskRequest;
import com.tasknotes.tasks.dto.UpdatePriorityRequest;
import com.tasknotes.tasks.dto.TaskResponse;
import com.tasknotes.tasks.model.TaskStatus;
import com.tasknotes.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Tarefas", description = "Gerenciamento de tarefas por categoria")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/api/categories/{categoryId}/tasks")
    @Operation(summary = "Listar tarefas de uma categoria (paginado por cursor)")
    public CursorPageResponse<TaskResponse> findByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) TaskStatus status) {
        return service.findByCategory(categoryId, cursor, status);
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

    @PatchMapping("/api/tasks/{id}/priority")
    @Operation(summary = "Atualizar prioridade de uma tarefa")
    public TaskResponse updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePriorityRequest request) {
        return service.updatePriority(id, request);
    }

    @PutMapping("/api/categories/{categoryId}/tasks/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reordenar tarefas de uma categoria")
    public void reorder(@PathVariable Long categoryId, @RequestBody List<Long> ids) {
        service.reorder(categoryId, ids);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir uma tarefa e suas subtarefas")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/api/tasks/tags")
    @Operation(summary = "Listar tags distintas do usuário atual")
    public List<Map<String, String>> getDistinctTags() {
        return service.getDistinctTags();
    }
}
