package com.tasknotes.concepts.controller;

import com.tasknotes.concepts.dto.AcceptConceptRequest;
import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import com.tasknotes.concepts.service.ProgrammingConceptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concepts")
@Validated
public class ProgrammingConceptController {

    private final ProgrammingConceptService service;

    public ProgrammingConceptController(ProgrammingConceptService service) {
        this.service = service;
    }

    @GetMapping("/suggest")
    public ConceptSuggestionResponse suggest(
            @RequestParam @NotBlank @Size(max = 120) String term) {
        return service.suggest(term);
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> accept(@RequestBody @Valid AcceptConceptRequest request) {
        service.accept(request);
        return ResponseEntity.ok().build();
    }
}
