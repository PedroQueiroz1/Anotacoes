package com.tasknotes.service;

import com.tasknotes.categories.dto.CategoryRequest;
import com.tasknotes.categories.dto.CategoryResponse;
import com.tasknotes.categories.service.CategoryService;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.categories.model.Category;
import com.tasknotes.tasks.model.TaskStatus;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.tasks.repository.TaskRepository;
import com.tasknotes.users.service.SecurityHelper;
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
class CategoryServiceTest {

    @Mock CategoryRepository repository;
    @Mock TaskRepository     taskRepository;
    @Mock SecurityHelper     securityHelper;

    @InjectMocks CategoryService service;

    private static final Long OWNER_ID = 1L;

    private AppUser stubOwner() {
        AppUser owner = mock(AppUser.class);
        lenient().when(owner.getId()).thenReturn(OWNER_ID);
        return owner;
    }

    private Category stub(Long id, String name) {
        AppUser owner = stubOwner();
        Category c = mock(Category.class);
        lenient().when(c.getId()).thenReturn(id);
        lenient().when(c.getUuid()).thenReturn(null);
        lenient().when(c.getName()).thenReturn(name);
        lenient().when(c.getOwner()).thenReturn(owner);
        lenient().when(c.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(c.getUpdatedAt()).thenReturn(LocalDateTime.now());
        return c;
    }

    @BeforeEach
    void setup() {
        lenient().when(securityHelper.currentUserId()).thenReturn(OWNER_ID);
        AppUser owner = stubOwner();
        lenient().when(securityHelper.currentAppUser()).thenReturn(owner);
        lenient().when(taskRepository.countByCategoryIdAndStatusNot(anyLong(), any()))
                 .thenReturn(0L);
    }

    // ── findById ──────────────────────────────────────────────────────────────
    @Test
    void findById_returnsResponse_whenExists() {
        Category cat = stub(1L, "Work");
        when(repository.findById(1L)).thenReturn(Optional.of(cat));

        CategoryResponse r = service.findById(1L);

        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.name()).isEqualTo("Work");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesAndReturnsResponse_whenUnderLimit() {
        Category saved = stub(4L, "New");
        when(repository.countByOwnerId(OWNER_ID)).thenReturn(3L);
        when(repository.save(any())).thenReturn(saved);

        CategoryResponse r = service.create(new CategoryRequest("New"));

        assertThat(r.name()).isEqualTo("New");
        verify(repository).save(any(Category.class));
    }

    @Test
    void create_throwsBusinessException_whenAtFiveCategories() {
        when(repository.countByOwnerId(OWNER_ID)).thenReturn(5L);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Extra")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");

        verify(repository, never()).save(any());
    }

    @Test
    void create_trimsName_beforeSaving() {
        when(repository.countByOwnerId(OWNER_ID)).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            return stub(1L, c.getName());
        });

        service.create(new CategoryRequest("  Trimmed  "));

        verify(repository).save(argThat(c -> "Trimmed".equals(c.getName())));
    }

    // ── update ────────────────────────────────────────────────────────────────
    @Test
    void update_changesNameAndSaves() {
        Category existing = stub(1L, "Old");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.update(1L, new CategoryRequest("New Name"));

        verify(existing).setName("New Name");
        verify(repository).save(existing);
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new CategoryRequest("X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        Category cat = stub(1L, "X");
        when(repository.findById(1L)).thenReturn(Optional.of(cat));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    // ── pendingTaskCount ──────────────────────────────────────────────────────
    @Test
    void findById_includesPendingTaskCount() {
        Category cat = stub(1L, "Work");
        when(repository.findById(1L)).thenReturn(Optional.of(cat));
        when(taskRepository.countByCategoryIdAndStatusNot(1L, TaskStatus.DONE)).thenReturn(3L);

        CategoryResponse r = service.findById(1L);

        assertThat(r.pendingTaskCount()).isEqualTo(3);
    }
}
