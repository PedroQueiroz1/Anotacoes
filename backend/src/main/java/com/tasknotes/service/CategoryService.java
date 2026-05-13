package com.tasknotes.service;

import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.TaskStatus;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private static final int MAX_CATEGORIES = 5;

    private final CategoryRepository repository;
    private final TaskRepository taskRepository;

    public CategoryService(CategoryRepository repository, TaskRepository taskRepository) {
        this.repository = repository;
        this.taskRepository = taskRepository;
    }

    public CategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
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
        int pending = (int) taskRepository.countByCategoryIdAndStatusNot(c.getId(), TaskStatus.DONE);
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                pending
        );
    }
}
