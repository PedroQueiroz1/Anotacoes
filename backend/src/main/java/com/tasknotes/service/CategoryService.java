package com.tasknotes.service;

import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private static final int MAX_CATEGORIES = 5;

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse create(CategoryRequest request) {
        if (repository.count() >= MAX_CATEGORIES) {
            throw new BusinessException(
                    "Limite máximo de " + MAX_CATEGORIES + " categorias atingido.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        return toResponse(repository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.name().trim());
        return toResponse(repository.save(category));
    }

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Category findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                0  // pendingTaskCount: calculado a partir da Etapa 3
        );
    }
}
