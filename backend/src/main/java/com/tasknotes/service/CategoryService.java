package com.tasknotes.service;

import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.TaskStatus;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.TaskRepository;
import com.tasknotes.util.SecurityHelper;
import com.tasknotes.util.UuidV7Generator;
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
    private final SecurityHelper securityHelper;

    public CategoryService(CategoryRepository repository,
                           TaskRepository taskRepository,
                           SecurityHelper securityHelper) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.securityHelper = securityHelper;
    }

    public CategoryResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public CategoryResponse findBySlug(String slug) {
        Long ownerId = securityHelper.currentUserId();
        Category c = repository.findBySlugAndOwnerId(slug, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + slug));
        return toResponse(c);
    }

    public List<CategoryResponse> findAll() {
        Long ownerId = securityHelper.currentUserId();
        return repository.findAllByOwnerIdOrdered(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void reorder(List<Long> ids) {
        Long ownerId = securityHelper.currentUserId();
        for (int i = 0; i < ids.size(); i++) {
            Category c = repository.findById(ids.get(i))
                    .filter(cat -> cat.getOwner() != null && ownerId.equals(cat.getOwner().getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
            repository.updatePosition(c.getId(), i);
        }
    }

    public CategoryResponse create(CategoryRequest request) {
        Long ownerId = securityHelper.currentUserId();
        if (repository.countByOwnerId(ownerId) >= MAX_CATEGORIES) {
            throw new BusinessException(
                    "Limite máximo de " + MAX_CATEGORIES + " categorias atingido.");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setSlug(uniqueSlugForOwner(generateSlug(request.name().trim()), null, ownerId));
        category.setOwner(securityHelper.currentAppUser());
        return toResponse(repository.save(category));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);
        Long ownerId = category.getOwner().getId();
        category.setName(request.name().trim());
        category.setSlug(uniqueSlugForOwner(generateSlug(request.name().trim()), id, ownerId));
        return toResponse(repository.save(category));
    }

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillSlugsAndUuids() {
        for (Category c : repository.findAll()) {
            boolean dirty = false;
            if (c.getSlug() == null || c.getSlug().isEmpty()) {
                c.setSlug(uniqueSlugLegacy(generateSlug(c.getName()), c.getId()));
                dirty = true;
            }
            if (c.getUuid() == null) {
                c.setUuid(UuidV7Generator.generate());
                dirty = true;
            }
            if (dirty) repository.save(c);
        }
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String ascii = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = ascii.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "categoria" : slug;
    }

    private String uniqueSlugForOwner(String base, Long excludeId, Long ownerId) {
        String candidate = base;
        int suffix = 2;
        while (excludeId != null
                ? repository.existsBySlugAndIdNotAndOwnerId(candidate, excludeId, ownerId)
                : repository.existsBySlugAndOwnerId(candidate, ownerId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String uniqueSlugLegacy(String base, Long excludeId) {
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
        Long ownerId = securityHelper.currentUserId();
        Category c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
        if (c.getOwner() == null || !ownerId.equals(c.getOwner().getId())) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + id);
        }
        return c;
    }

    private CategoryResponse toResponse(Category c) {
        int pending = (int) taskRepository.countByCategoryIdAndStatusNot(c.getId(), TaskStatus.DONE);
        return new CategoryResponse(
                c.getId(),
                c.getUuid(),
                c.getName(),
                c.getSlug(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                pending
        );
    }
}
