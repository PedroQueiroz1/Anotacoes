package com.tasknotes.service;

import com.tasknotes.dto.SubtaskRequest;
import com.tasknotes.dto.SubtaskResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Subtask;
import com.tasknotes.repository.SubtaskRepository;
import com.tasknotes.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubtaskService {

    private static final int MAX_SUBTASKS = 20;

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;

    public SubtaskService(SubtaskRepository subtaskRepository, TaskRepository taskRepository) {
        this.subtaskRepository = subtaskRepository;
        this.taskRepository = taskRepository;
    }

    public List<SubtaskResponse> findByTask(Long taskId) {
        validateTask(taskId);
        return subtaskRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SubtaskResponse create(Long taskId, SubtaskRequest request) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada: " + taskId));

        if (subtaskRepository.countByTaskId(taskId) >= MAX_SUBTASKS) {
            throw new BusinessException("Limite máximo de " + MAX_SUBTASKS + " subtarefas por tarefa.");
        }

        Subtask subtask = new Subtask();
        subtask.setTask(task);
        subtask.setText(request.text().trim());
        return toResponse(subtaskRepository.save(subtask));
    }

    public SubtaskResponse toggle(Long id) {
        Subtask subtask = findOrThrow(id);
        subtask.setDone(!subtask.isDone());
        return toResponse(subtaskRepository.save(subtask));
    }

    public void delete(Long id) {
        findOrThrow(id);
        subtaskRepository.deleteById(id);
    }

    private Subtask findOrThrow(Long id) {
        return subtaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subtarefa não encontrada: " + id));
    }

    private void validateTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Tarefa não encontrada: " + taskId);
        }
    }

    private SubtaskResponse toResponse(Subtask s) {
        return new SubtaskResponse(
                s.getId(),
                s.getTask().getId(),
                s.getText(),
                s.isDone(),
                s.getCreatedAt()
        );
    }
}
