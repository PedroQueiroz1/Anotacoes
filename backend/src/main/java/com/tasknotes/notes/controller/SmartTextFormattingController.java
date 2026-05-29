package com.tasknotes.notes.controller;

import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.notes.dto.SmartFormatApplyRequest;
import com.tasknotes.notes.dto.SmartFormatPreviewResponse;
import com.tasknotes.notes.service.SmartTextFormattingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anotacoes/{id}/smart-format")
public class SmartTextFormattingController {

    private final SmartTextFormattingService service;

    public SmartTextFormattingController(SmartTextFormattingService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public ResponseEntity<SmartFormatPreviewResponse> preview(@PathVariable Long id) {
        return ResponseEntity.ok(service.preview(id));
    }

    @PostMapping("/apply")
    public ResponseEntity<NoteResponse> apply(
            @PathVariable Long id,
            @Valid @RequestBody SmartFormatApplyRequest request) {
        return ResponseEntity.ok(service.apply(id, request));
    }
}
