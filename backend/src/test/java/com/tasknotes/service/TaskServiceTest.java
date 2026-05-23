package com.tasknotes.service;

import com.tasknotes.tasks.dto.StatusUpdateRequest;
import com.tasknotes.tasks.dto.TaskRequest;
import com.tasknotes.tasks.dto.TaskResponse;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.categories.model.Category;
import com.tasknotes.tasks.model.Priority;
import com.tasknotes.tasks.model.Task;
import com.tasknotes.tasks.model.TaskStatus;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.tasks.repository.TaskRepository;
import com.tasknotes.users.service.SecurityHelper;
import com.tasknotes.tasks.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository     taskRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock SecurityHelper     securityHelper;

    @InjectMocks TaskService service;

    private static final Long OWNER_ID = 1L;

    private Category stubCategory(Long id) {
        AppUser owner = mock(AppUser.class);
        lenient().when(owner.getId()).thenReturn(OWNER_ID);
        Category c = mock(Category.class);
        lenient().when(c.getId()).thenReturn(id);
        lenient().when(c.getOwner()).thenReturn(owner);
        return c;
    }

    private Task stubTask(Long id, Priority priority, TaskStatus status) {
        Category cat = stubCategory(1L);
        Task t = mock(Task.class);
        lenient().when(t.getId()).thenReturn(id);
        lenient().when(t.getUuid()).thenReturn(null);
        lenient().when(t.getCategory()).thenReturn(cat);
        lenient().when(t.getTitle()).thenReturn("Task " + id);
        lenient().when(t.getDescription()).thenReturn(null);
        lenient().when(t.getDueDate()).thenReturn(null);
        lenient().when(t.getPriority()).thenReturn(priority);
        lenient().when(t.getStatus()).thenReturn(status);
        lenient().when(t.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(t.getUpdatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(t.getTagName()).thenReturn(null);
        lenient().when(t.getTagColor()).thenReturn(null);
        return t;
    }

    @BeforeEach
    void setup() {
        lenient().when(securityHelper.currentUserId()).thenReturn(OWNER_ID);
    }

    // ── findByCategory ────────────────────────────────────────────────────────
    @Test
    void findByCategory_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findByIdWithOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCategory(99L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesAndReturnsTask() {
        Category cat  = stubCategory(1L);
        Task     saved = stubTask(10L, Priority.MEDIUM, TaskStatus.TODO);
        when(categoryRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(cat));
        when(taskRepository.save(any())).thenReturn(saved);

        TaskResponse r = service.create(1L, new TaskRequest("Buy milk", null, null, Priority.MEDIUM, null, null));

        assertThat(r.id()).isEqualTo(10L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void create_defaultsPriorityToLow_whenNull() {
        Category cat = stubCategory(1L);
        when(categoryRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(cat));
        when(taskRepository.save(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return stubTask(1L, t.getPriority(), TaskStatus.TODO);
        });

        TaskResponse r = service.create(1L, new TaskRequest("Task", null, null, null, null, null));

        assertThat(r.priority()).isEqualTo(Priority.LOW);
    }

    @Test
    void create_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findByIdWithOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new TaskRequest("X", null, null, Priority.LOW, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────
    @Test
    void update_savesChangedFields() {
        Task existing = stubTask(1L, Priority.LOW, TaskStatus.TODO);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        service.update(1L, new TaskRequest("Updated Title", "Desc", null, Priority.HIGH, null, null));

        verify(existing).setTitle("Updated Title");
        verify(existing).setDescription("Desc");
        verify(existing).setPriority(Priority.HIGH);
        verify(taskRepository).save(existing);
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(taskRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new TaskRequest("X", null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateStatus ──────────────────────────────────────────────────────────
    @Test
    void updateStatus_setsNewStatusAndSaves() {
        Task existing = stubTask(1L, Priority.MEDIUM, TaskStatus.TODO);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        service.updateStatus(1L, new StatusUpdateRequest(TaskStatus.DONE));

        verify(existing).setStatus(TaskStatus.DONE);
        verify(taskRepository).save(existing);
    }

    @Test
    void updateStatus_throwsResourceNotFoundException_whenNotFound() {
        when(taskRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(99L, new StatusUpdateRequest(TaskStatus.DONE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        Task existing = stubTask(1L, Priority.LOW, TaskStatus.TODO);
        when(taskRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        verify(taskRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(taskRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(taskRepository, never()).deleteById(any());
    }
}
