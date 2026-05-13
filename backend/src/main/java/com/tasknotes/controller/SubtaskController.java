package com.tasknotes.controller;

import com.tasknotes.dto.SubtaskRequest;
import com.tasknotes.dto.SubtaskResponse;
import com.tasknotes.service.SubtaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Subtarefas", description = "Checklist de subtarefas por tarefa")
public class SubtaskController {

    private final SubtaskService service;

    public SubtaskController(SubtaskService service) {
        this.service = service;
    }

    @GetMapping("/api/tasks/{taskId}/subtasks")
    @Operation(summary = "Listar subtarefas de uma tarefa")
    public List<SubtaskResponse> findByTask(@PathVariable Long taskId) {
        return service.findByTask(taskId);
    }

    @PostMapping("/api/tasks/{taskId}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar subtarefa a uma tarefa (máx. 20)")
    public SubtaskResponse create(
            @PathVariable Long taskId,
            @Valid @RequestBody SubtaskRequest request) {
        return service.create(taskId, request);
    }

    @PatchMapping("/api/subtasks/{id}/toggle")
    @Operation(summary = "Marcar/desmarcar subtarefa como concluída")
    public SubtaskResponse toggle(@PathVariable Long id) {
        return service.toggle(id);
    }

    @DeleteMapping("/api/subtasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover uma subtarefa")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
