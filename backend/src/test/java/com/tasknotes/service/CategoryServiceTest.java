package com.tasknotes.service;

import com.tasknotes.dto.CategoryRequest;
import com.tasknotes.dto.CategoryResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.TaskStatus;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
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
class CategoryServiceTest {

    @Mock CategoryRepository repository;
    @Mock TaskRepository     taskRepository;

    @InjectMocks CategoryService service;

    private Category stub(Long id, String name) {
        Category c = mock(Category.class);
        when(c.getId()).thenReturn(id);
        when(c.getName()).thenReturn(name);
        when(c.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(c.getUpdatedAt()).thenReturn(LocalDateTime.now());
        return c;
    }

    @BeforeEach
    void setup() {
        lenient().when(taskRepository.countByCategoryIdAndStatusNot(anyLong(), any()))
                 .thenReturn(0L);
    }

    // ── findAll ───────────────────────────────────────────────────────────────
    @Test
    void findAll_returnsAllCategoriesAsResponses() {
        when(repository.findAllByOrderByCreatedAtAsc())
                .thenReturn(List.of(stub(1L, "Work"), stub(2L, "Personal")));

        List<CategoryResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::name)
                          .containsExactly("Work", "Personal");
    }

    @Test
    void findAll_returnsEmpty_whenNoCategoriesExist() {
        when(repository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    // ── findById ──────────────────────────────────────────────────────────────
    @Test
    void findById_returnsResponse_whenExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(stub(1L, "Work")));

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
        when(repository.count()).thenReturn(3L);
        when(repository.save(any())).thenReturn(stub(4L, "New"));

        CategoryResponse r = service.create(new CategoryRequest("New"));

        assertThat(r.name()).isEqualTo("New");
        verify(repository).save(any(Category.class));
    }

    @Test
    void create_throwsBusinessException_whenAtFiveCategories() {
        when(repository.count()).thenReturn(5L);

        assertThatThrownBy(() -> service.create(new CategoryRequest("Extra")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5");

        verify(repository, never()).save(any());
    }

    @Test
    void create_trimsName_beforeSaving() {
        when(repository.count()).thenReturn(0L);
        when(repository.save(any())).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            Category saved = stub(1L, c.getName());
            return saved;
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
        when(repository.findById(1L)).thenReturn(Optional.of(stub(1L, "X")));

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
        when(repository.findById(1L)).thenReturn(Optional.of(stub(1L, "Work")));
        when(taskRepository.countByCategoryIdAndStatusNot(1L, TaskStatus.DONE)).thenReturn(3L);

        CategoryResponse r = service.findById(1L);

        assertThat(r.pendingTaskCount()).isEqualTo(3);
    }
}
