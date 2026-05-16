package com.tasknotes.service;

import com.tasknotes.dto.SubtaskPageResponse;
import com.tasknotes.dto.SubtaskRequest;
import com.tasknotes.dto.SubtaskResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Subtask;
import com.tasknotes.repository.SubtaskRepository;
import com.tasknotes.repository.TaskRepository;
import com.tasknotes.util.CursorCodec;
import com.tasknotes.util.UuidV7Generator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SubtaskService {

    private static final int MAX_SUBTASKS = 20;
    private static final int SUBTASK_LIMIT = 20;

    private final SubtaskRepository subtaskRepository;
    private final TaskRepository taskRepository;

    public SubtaskService(SubtaskRepository subtaskRepository, TaskRepository taskRepository) {
        this.subtaskRepository = subtaskRepository;
        this.taskRepository = taskRepository;
    }

    public SubtaskPageResponse findByTask(Long taskId, String cursor) {
        validateTask(taskId);
        int fetch = SUBTASK_LIMIT + 1;
        var pageable = PageRequest.of(0, fetch);
        List<Subtask> raw;

        if (cursor == null) {
            raw = subtaskRepository.findByTaskFirstPage(taskId, pageable);
        } else {
            CursorCodec.CursorPayload p = CursorCodec.decode(cursor);
            if (p == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor de paginação inválido.");
            raw = subtaskRepository.findByTaskAfterCursor(taskId, p.createdAt(), p.id(), pageable);
        }

        boolean hasNext = raw.size() > SUBTASK_LIMIT;
        List<Subtask> page = hasNext ? raw.subList(0, SUBTASK_LIMIT) : raw;
        String nextCursor = hasNext
                ? CursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId())
                : null;

        long totalCount = subtaskRepository.countByTaskId(taskId);
        long completedCount = subtaskRepository.countByTaskIdAndDone(taskId, true);

        return new SubtaskPageResponse(
                page.stream().map(this::toResponse).toList(),
                nextCursor,
                hasNext,
                SUBTASK_LIMIT,
                totalCount,
                completedCount
        );
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

    public SubtaskResponse update(Long id, SubtaskRequest request) {
        Subtask subtask = findOrThrow(id);
        subtask.setText(request.text().trim());
        return toResponse(subtaskRepository.save(subtask));
    }

    public void delete(Long id) {
        findOrThrow(id);
        subtaskRepository.deleteById(id);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillUuids() {
        for (Subtask s : subtaskRepository.findAll()) {
            if (s.getUuid() == null) {
                s.setUuid(UuidV7Generator.generate());
                subtaskRepository.save(s);
            }
        }
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
                s.getUuid(),
                s.getTask().getId(),
                s.getText(),
                s.isDone(),
                s.getCreatedAt()
        );
    }
}
