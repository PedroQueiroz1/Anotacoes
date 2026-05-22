package com.tasknotes.service;

import com.tasknotes.tasks.dto.SubtaskPageResponse;
import com.tasknotes.tasks.dto.SubtaskRequest;
import com.tasknotes.tasks.dto.SubtaskResponse;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.categories.model.Category;
import com.tasknotes.tasks.model.Subtask;
import com.tasknotes.tasks.model.Task;
import com.tasknotes.tasks.repository.SubtaskRepository;
import com.tasknotes.tasks.repository.TaskRepository;
import com.tasknotes.users.service.SecurityHelper;
import com.tasknotes.tasks.service.SubtaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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
    @Mock SecurityHelper    securityHelper;

    @InjectMocks SubtaskService service;

    private static final Long OWNER_ID = 1L;

    private Task stubTask(Long id) {
        AppUser owner = mock(AppUser.class);
        lenient().when(owner.getId()).thenReturn(OWNER_ID);
        Category cat = mock(Category.class);
        lenient().when(cat.getOwner()).thenReturn(owner);
        Task t = mock(Task.class);
        lenient().when(t.getId()).thenReturn(id);
        lenient().when(t.getUuid()).thenReturn(null);
        lenient().when(t.getCategory()).thenReturn(cat);
        return t;
    }

    private Subtask stubSubtask(Long id, Long taskId, String text, boolean done) {
        Task    task = stubTask(taskId);
        Subtask s    = mock(Subtask.class);
        lenient().when(s.getId()).thenReturn(id);
        lenient().when(s.getUuid()).thenReturn(null);
        lenient().when(s.getTask()).thenReturn(task);
        lenient().when(s.getText()).thenReturn(text);
        lenient().when(s.isDone()).thenReturn(done);
        lenient().when(s.getCreatedAt()).thenReturn(LocalDateTime.now());
        return s;
    }

    @BeforeEach
    void setup() {
        lenient().when(securityHelper.currentUserId()).thenReturn(OWNER_ID);
    }

    // ── findByTask ────────────────────────────────────────────────────────────
    @Test
    void findByTask_returnsPage_whenTaskExists() {
        Task task = stubTask(1L);
        Subtask sub1 = stubSubtask(1L, 1L, "Buy milk", false);
        Subtask sub2 = stubSubtask(2L, 1L, "Pay bill", true);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(task));
        when(subtaskRepository.findByTaskFirstPage(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(sub1, sub2));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(2L);
        when(subtaskRepository.countByTaskIdAndDone(1L, true)).thenReturn(1L);

        SubtaskPageResponse result = service.findByTask(1L, null);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).text()).isEqualTo("Buy milk");
        assertThat(result.items().get(1).done()).isTrue();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.completedCount()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByTask_throwsResourceNotFoundException_whenTaskMissing() {
        when(taskRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByTask(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesSubtask_whenUnderLimit() {
        Task    task  = stubTask(1L);
        Subtask saved = stubSubtask(10L, 1L, "New sub", false);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(task));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(5L);
        when(subtaskRepository.save(any())).thenReturn(saved);

        SubtaskResponse r = service.create(1L, new SubtaskRequest("New sub"));

        assertThat(r.text()).isEqualTo("New sub");
        assertThat(r.done()).isFalse();
        verify(subtaskRepository).save(any(Subtask.class));
    }

    @Test
    void create_throwsBusinessException_whenAtTwentySubtasks() {
        Task task = stubTask(1L);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(task));
        when(subtaskRepository.countByTaskId(1L)).thenReturn(20L);

        assertThatThrownBy(() -> service.create(1L, new SubtaskRequest("Extra")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("20");

        verify(subtaskRepository, never()).save(any());
    }

    @Test
    void create_throwsResourceNotFoundException_whenTaskMissing() {
        when(taskRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new SubtaskRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_trimsText_beforeSaving() {
        Task task = stubTask(1L);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(task));
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
        when(subtaskRepository.findByIdWithTaskCategoryAndOwner(1L)).thenReturn(Optional.of(subtask));
        when(subtaskRepository.save(subtask)).thenReturn(subtask);

        service.toggle(1L);

        verify(subtask).setDone(true);
        verify(subtaskRepository).save(subtask);
    }

    @Test
    void toggle_invertsAndSaves_whenTrue() {
        Subtask subtask = stubSubtask(1L, 1L, "Task", true);
        when(subtaskRepository.findByIdWithTaskCategoryAndOwner(1L)).thenReturn(Optional.of(subtask));
        when(subtaskRepository.save(subtask)).thenReturn(subtask);

        service.toggle(1L);

        verify(subtask).setDone(false);
        verify(subtaskRepository).save(subtask);
    }

    @Test
    void toggle_throwsResourceNotFoundException_whenNotFound() {
        when(subtaskRepository.findByIdWithTaskCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggle(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        Subtask existing = stubSubtask(1L, 1L, "X", false);
        when(subtaskRepository.findByIdWithTaskCategoryAndOwner(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(subtaskRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(subtaskRepository.findByIdWithTaskCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(subtaskRepository, never()).deleteById(any());
    }
}
