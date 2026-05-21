package com.tasknotes.tasks.service;

import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.tasks.dto.StatusUpdateRequest;
import com.tasknotes.tasks.dto.TaskRequest;
import com.tasknotes.tasks.dto.UpdatePriorityRequest;
import com.tasknotes.tasks.dto.TaskResponse;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.categories.model.Category;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.tasks.model.Priority;
import com.tasknotes.tasks.model.Task;
import com.tasknotes.tasks.model.TaskStatus;
import com.tasknotes.tasks.repository.TaskRepository;
import com.tasknotes.shared.util.CursorCodec;
import com.tasknotes.users.service.SecurityHelper;
import com.tasknotes.shared.util.UuidV7Generator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TaskService {

    private static final int TASK_LIMIT = 10;

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityHelper securityHelper;

    public TaskService(TaskRepository taskRepository,
                       CategoryRepository categoryRepository,
                       SecurityHelper securityHelper) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.securityHelper = securityHelper;
    }

    public CursorPageResponse<TaskResponse> findByCategory(Long categoryId, String cursor, TaskStatus statusFilter) {
        validateCategoryOwnership(categoryId);
        int fetch = TASK_LIMIT + 1;
        var pageable = PageRequest.of(0, fetch);
        List<Task> raw;

        if (cursor == null) {
            raw = statusFilter == null
                    ? taskRepository.findByCategoryFirstPage(categoryId, pageable)
                    : taskRepository.findByCategoryFirstPageByStatus(categoryId, statusFilter.name(), pageable);
        } else {
            CursorCodec.CursorPayload p = CursorCodec.decode(cursor);
            if (p == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor de paginação inválido.");
            raw = statusFilter == null
                    ? taskRepository.findByCategoryAfterCursor(categoryId, p.createdAt(), p.id(), pageable)
                    : taskRepository.findByCategoryAfterCursorByStatus(categoryId, statusFilter.name(), p.createdAt(), p.id(), pageable);
        }

        boolean hasNext = raw.size() > TASK_LIMIT;
        List<Task> page = hasNext ? raw.subList(0, TASK_LIMIT) : raw;
        String nextCursor = hasNext
                ? CursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId())
                : null;

        return new CursorPageResponse<>(page.stream().map(this::toResponse).toList(), nextCursor, hasNext, TASK_LIMIT, null);
    }

    public TaskResponse create(Long categoryId, TaskRequest request) {
        Category category = findCategoryWithOwnership(categoryId);

        Task task = new Task();
        task.setCategory(category);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() != null ? request.priority() : Priority.LOW);
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        if (request.priority() != null) task.setPriority(request.priority());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateStatus(Long id, StatusUpdateRequest request) {
        Task task = findOrThrow(id);
        task.setStatus(request.status());
        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updatePriority(Long id, UpdatePriorityRequest request) {
        Task task = findOrThrow(id);
        task.setPriority(request.priority());
        return toResponse(taskRepository.save(task));
    }

    public void delete(Long id) {
        findOrThrow(id);
        taskRepository.deleteById(id);
    }

    @Transactional
    public void reorder(Long categoryId, List<Long> ids) {
        validateCategoryOwnership(categoryId);
        for (int i = 0; i < ids.size(); i++) {
            taskRepository.updatePosition(ids.get(i), i);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillUuids() {
        for (Task t : taskRepository.findAll()) {
            if (t.getUuid() == null) {
                t.setUuid(UuidV7Generator.generate());
                taskRepository.save(t);
            }
        }
    }

    private Task findOrThrow(Long id) {
        Long ownerId = securityHelper.currentUserId();
        Task task = taskRepository.findByIdWithCategoryAndOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + id));
        Category cat = task.getCategory();
        if (cat.getOwner() == null || !ownerId.equals(cat.getOwner().getId())) {
            throw new ResourceNotFoundException("Tarefa não encontrada: " + id);
        }
        return task;
    }

    private void validateCategoryOwnership(Long categoryId) {
        findCategoryWithOwnership(categoryId);
    }

    private Category findCategoryWithOwnership(Long categoryId) {
        Long ownerId = securityHelper.currentUserId();
        Category cat = categoryRepository.findByIdWithOwner(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
        if (cat.getOwner() == null || !ownerId.equals(cat.getOwner().getId())) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + categoryId);
        }
        return cat;
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getUuid(),
                t.getCategory().getId(),
                t.getTitle(),
                t.getDescription(),
                t.getDueDate(),
                t.getPriority(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
