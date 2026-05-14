package com.tasknotes.service;

import com.tasknotes.dto.StatusUpdateRequest;
import com.tasknotes.dto.TaskRequest;
import com.tasknotes.dto.TaskResponse;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Priority;
import com.tasknotes.model.Task;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public TaskService(TaskRepository taskRepository, CategoryRepository categoryRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<TaskResponse> findByCategory(Long categoryId) {
        validateCategory(categoryId);
        return taskRepository.findByCategoryIdOrdered(categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse create(Long categoryId, TaskRequest request) {
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));

        Task task = new Task();
        task.setCategory(category);
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
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

    public void delete(Long id) {
        findOrThrow(id);
        taskRepository.deleteById(id);
    }

    @Transactional
    public void reorder(Long categoryId, List<Long> ids) {
        validateCategory(categoryId);
        for (int i = 0; i < ids.size(); i++) {
            taskRepository.updatePosition(ids.get(i), i);
        }
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + id));
    }

    private void validateCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + categoryId);
        }
    }

    private TaskResponse toResponse(Task t) {
        return new TaskResponse(
                t.getId(),
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
