package com.tasknotes.dictionary.controller;

import com.tasknotes.dictionary.dto.DictionaryEntryRequest;
import com.tasknotes.dictionary.dto.DictionaryEntryResponse;
import com.tasknotes.dictionary.service.DictionaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {

    private final DictionaryService service;

    public DictionaryController(DictionaryService service) {
        this.service = service;
    }

    @GetMapping
    public List<DictionaryEntryResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryEntryResponse create(@Valid @RequestBody DictionaryEntryRequest request) {
        return service.create(request);
    }
}
