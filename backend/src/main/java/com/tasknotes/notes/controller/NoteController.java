package com.tasknotes.notes.controller;

import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.notes.dto.NoteRequest;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.notes.dto.NoteTagRequest;
import com.tasknotes.notes.dto.NoteTagResponse;
import com.tasknotes.notes.service.NoteService;
import com.tasknotes.notes.service.NoteTagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class NoteController {

    private final NoteService service;
    private final NoteTagService tagService;

    public NoteController(NoteService service, NoteTagService tagService) {
        this.service = service;
        this.tagService = tagService;
    }

    @GetMapping("/api/categories/{categoryId}/notes")
    public CursorPageResponse<NoteResponse> listByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            @RequestParam(required = false) Long tagId) {
        return service.findByCategory(categoryId, cursor, query, sort, tagId);
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
    public void reorder(@PathVariable Long categoryId,
                        @RequestParam(defaultValue = "0") int offset,
                        @RequestBody List<Long> ids) {
        service.reorder(categoryId, ids, offset);
    }

    @DeleteMapping("/api/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ── Pin ───────────────────────────────────────────────────────────────────
    @PatchMapping("/api/notes/{id}/pin")
    public NoteResponse togglePin(@PathVariable Long id) {
        return service.togglePin(id);
    }

    // ── Move ──────────────────────────────────────────────────────────────────
    @PatchMapping("/api/notes/{id}/move-to-top")
    public NoteResponse moveToTop(@PathVariable Long id) {
        return service.moveToTop(id);
    }

    @PatchMapping("/api/notes/{id}/move-to-bottom")
    public NoteResponse moveToBottom(@PathVariable Long id) {
        return service.moveToBottom(id);
    }

    @PatchMapping("/api/notes/{id}/move-to-position")
    public NoteResponse moveToPosition(@PathVariable Long id,
                                       @RequestBody Map<String, Integer> body) {
        int position = body.getOrDefault("position", 1);
        return service.moveToPosition(id, position);
    }

    // ── Tags ──────────────────────────────────────────────────────────────────
    @GetMapping("/api/notes/tags")
    public List<NoteTagResponse> listTags() {
        return tagService.listTags();
    }

    @PostMapping("/api/notes/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteTagResponse createTag(@Valid @RequestBody NoteTagRequest request) {
        return tagService.createTag(request);
    }

    @DeleteMapping("/api/notes/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
    }
}
