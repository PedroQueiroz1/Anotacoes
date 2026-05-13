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

import java.util.Comparator;
import java.util.List;

@Service
public class TaskService {

    // HIGH → 0 (topo), MEDIUM → 1, LOW → 2
    private static final Comparator<Task> BY_PRIORITY =
            Comparator.comparingInt(t -> switch (t.getPriority()) {
                case HIGH   -> 0;
                case MEDIUM -> 1;
                case LOW    -> 2;
            });

    private static final Comparator<Task> ORDER =
            BY_PRIORITY.thenComparing(Task::getCreatedAt);

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public TaskService(TaskRepository taskRepository, CategoryRepository categoryRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<TaskResponse> findByCategory(Long categoryId) {
        validateCategory(categoryId);
        return taskRepository.findByCategoryId(categoryId)
                .stream()
                .sorted(ORDER)
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
