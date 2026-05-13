package com.tasknotes.service;

import com.tasknotes.dto.SubtaskRequest;
import com.tasknotes.dto.SubtaskResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Subtask;
import com.tasknotes.model.Task;
import com.tasknotes.repository.SubtaskRepository;
import com.tasknotes.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubtaskServiceTest {

    @Mock SubtaskRepository subtaskRepository;
    @Mock TaskRepository    taskRepository;

    @InjectMocks SubtaskService service;

    private Task stubTask(Long id) {
        Task t = mock(Task.class);
        when(t.getId()).thenReturn(id);
        return t;
    }

    private Subtask stubSubtask(Long id, Long taskId, String text, boolean done) {
        Subtask s = mock(Subtask.class);
        when(s.getId()).thenReturn(id);
        when(s.getTask()).thenReturn(stubTask(taskId));
        when(s.getText()).thenReturn(text);
        when(s.isDone()).thenReturn(done);
        when(s.getCreatedAt()).thenReturn(LocalDateTime.now());
        return s;
    }

    // ── findByTask ────────────────────────────────────────────────────────────
    @Test
    void findByTask_returnsList_whenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);
        when(subtaskRepository.findByTaskIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(
                        stubSubtask(1L, 1L, "Buy milk", false),
                        stubSubtask(2L, 1L, "Pay bill", true)
                ));

        List<SubtaskResponse> result = service.findByTask(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).text()).isEqualTo("Buy milk");
        assertThat(result.get(1).done()).isTrue();
    }

    @Test
    void findByTask_throwsResourceNotFoundException_whenTaskMissing() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByTask(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesSubtask_whenUnderLimit() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(stubTask(1L)));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(5L);
        Subtask saved = stubSubtask(10L, 1L, "New sub", false);
        when(subtaskRepository.save(any())).thenReturn(saved);

        SubtaskResponse r = service.create(1L, new SubtaskRequest("New sub"));

        assertThat(r.text()).isEqualTo("New sub");
        assertThat(r.done()).isFalse();
        verify(subtaskRepository).save(any(Subtask.class));
    }

    @Test
    void create_throwsBusinessException_whenAtTwentySubtasks() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(stubTask(1L)));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(20L);

        assertThatThrownBy(() -> service.create(1L, new SubtaskRequest("Extra")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20");

        verify(subtaskRepository, never()).save(any());
    }

    @Test
    void create_throwsResourceNotFoundException_whenTaskMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new SubtaskRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_trimsText_beforeSaving() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(stubTask(1L)));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(0L);
        when(subtaskRepository.save(any())).thenAnswer(inv -> {
            Subtask s = inv.getArgument(0);
            return stubSubtask(1L, 1L, s.getText(), false);
        });

        service.create(1L, new SubtaskRequest("  Trimmed  "));

        verify(subtaskRepository).save(argThat(s -> "Trimmed".equals(s.getText())));
    }

    // ── toggle ────────────────────────────────────────────────────────────────
    @Test
    void toggle_invertsAndSaves_whenFalse() {
        Subtask subtask = stubSubtask(1L, 1L, "Task", false);
        when(subtaskRepository.findById(1L)).thenReturn(Optional.of(subtask));
        when(subtaskRepository.save(subtask)).thenReturn(subtask);

        service.toggle(1L);

        verify(subtask).setDone(true);
        verify(subtaskRepository).save(subtask);
    }

    @Test
    void toggle_invertsAndSaves_whenTrue() {
        Subtask subtask = stubSubtask(1L, 1L, "Task", true);
        when(subtaskRepository.findById(1L)).thenReturn(Optional.of(subtask));
        when(subtaskRepository.save(subtask)).thenReturn(subtask);

        service.toggle(1L);

        verify(subtask).setDone(false);
        verify(subtaskRepository).save(subtask);
    }

    @Test
    void toggle_throwsResourceNotFoundException_whenNotFound() {
        when(subtaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        when(subtaskRepository.findById(1L)).thenReturn(Optional.of(stubSubtask(1L, 1L, "X", false)));

        service.delete(1L);

        verify(subtaskRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(subtaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subtaskRepository, never()).deleteById(any());
    }
}
