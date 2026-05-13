package com.tasknotes.controller;

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

    @GetMapping("/api/notes/{id}")
    public NoteResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/api/categories/{categoryId}/notes")
    public List<NoteResponse> listByCategory(@PathVariable Long categoryId) {
        return service.findByCategory(categoryId);
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

    @DeleteMapping("/api/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
