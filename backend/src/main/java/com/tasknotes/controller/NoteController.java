package com.tasknotes.controller;

import com.tasknotes.dto.CursorPageResponse;
import com.tasknotes.dto.NoteRequest;
import com.tasknotes.dto.NoteResponse;
import com.tasknotes.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @GetMapping("/api/categories/{categoryId}/notes")
    public CursorPageResponse<NoteResponse> listByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String cursor) {
        return service.findByCategory(categoryId, cursor);
    }

    @PostMapping("/api/categories/{categoryId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@PathVariable Long categoryId,
                               @Valid @RequestBody NoteRequest request) {
        return service.create(categoryId, request);
    }

    @PutMapping("/api/notes/{id}")
    public NoteResponse update(@PathVariable Long id,
                               @Valid @RequestBody NoteRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/api/categories/{categoryId}/notes/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable Long categoryId, @RequestBody List<Long> ids) {
        service.reorder(categoryId, ids);
    }

    @DeleteMapping("/api/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
