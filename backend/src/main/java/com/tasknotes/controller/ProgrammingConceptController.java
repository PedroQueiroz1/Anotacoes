package com.tasknotes.controller;

import com.tasknotes.dto.AcceptConceptRequest;
import com.tasknotes.dto.ConceptSuggestionResponse;
import com.tasknotes.service.ProgrammingConceptService;
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

    /**
     * GET /api/concepts/suggest?term=POJO&semicolonTrigger=true
     * Only the term is sent — never the full note content.
     * semicolonTrigger=true bypasses the technical-term heuristic for explicit ; lookups.
     */
    @GetMapping("/suggest")
    public ConceptSuggestionResponse suggest(
            @RequestParam @NotBlank @Size(max = 80) String term,
            @RequestParam(required = false, defaultValue = "false") boolean semicolonTrigger) {
        return service.suggest(term, semicolonTrigger);
    }

    /**
     * POST /api/concepts/accept
     * Records that the user accepted a suggestion; saves to local DB if new.
     */
    @PostMapping("/accept")
    public ResponseEntity<Void> accept(@RequestBody @Valid AcceptConceptRequest request) {
        service.accept(request);
        return ResponseEntity.ok().build();
    }
}
