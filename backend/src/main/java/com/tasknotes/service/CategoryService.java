package com.tasknotes.service;

import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.TaskStatus;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.TaskRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
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

    public CategoryResponse findBySlug(String slug) {
        Category c = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + slug));
        return toResponse(c);
    }

    public List<CategoryResponse> findAll() {
        return repository.findAllOrdered().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void reorder(List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            repository.updatePosition(ids.get(i), i);
        }
    }

    public CategoryResponse create(CategoryRequest request) {
        if (repository.count() >= MAX_CATEGORIES) {
            throw new BusinessException(
                    "Limite máximo de " + MAX_CATEGORIES + " categorias atingido.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(uniqueSlug(generateSlug(request.name().trim()), null));
        return toResponse(repository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        category.setName(request.name().trim());
        category.setSlug(uniqueSlug(generateSlug(request.name().trim()), id));
        return toResponse(repository.save(category));
    }

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    // Backfill slugs for categories created before this field was added
    @EventListener(ApplicationReadyEvent.class)
    public void backfillSlugs() {
        for (Category c : repository.findAll()) {
            if (c.getSlug() == null || c.getSlug().isEmpty()) {
                c.setSlug(uniqueSlug(generateSlug(c.getName()), c.getId()));
                repository.save(c);
            }
        }
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String ascii = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "categoria" : slug;
    }

    private String uniqueSlug(String base, Long excludeId) {
        String candidate = base;
        int suffix = 2;
        while (excludeId != null
                ? repository.existsBySlugAndIdNot(candidate, excludeId)
                : repository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
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
                c.getSlug(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                pending
        );
    }
}
