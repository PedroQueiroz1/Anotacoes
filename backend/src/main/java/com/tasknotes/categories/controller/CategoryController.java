package com.tasknotes.categories.controller;

import com.tasknotes.categories.dto.CategoryRequest;
import com.tasknotes.categories.dto.CategoryResponse;
import com.tasknotes.categories.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Gerenciamento de categorias (máx. 5)")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por ID")
    public CategoryResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Buscar categoria por slug")
    public CategoryResponse findBySlug(@PathVariable String slug) {
        return service.findBySlug(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar uma nova categoria")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar o nome de uma categoria")
    public CategoryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @PutMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reordenar categorias")
    public void reorder(@RequestBody List<Long> ids) {
        service.reorder(ids);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir uma categoria e todo o seu conteúdo")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
