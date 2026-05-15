package com.tasknotes.controller;

import com.tasknotes.dto.SearchResultDTO;
import com.tasknotes.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Busca", description = "Busca global em categorias, tarefas e anotações")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping("/api/search")
    @Operation(summary = "Busca em categorias, tarefas (título/descrição/subtarefas) e anotações")
    public List<SearchResultDTO> search(@RequestParam String query) {
        return service.search(query);
    }
}
