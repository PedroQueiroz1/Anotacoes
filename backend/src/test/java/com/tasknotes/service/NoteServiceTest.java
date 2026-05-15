package com.tasknotes.service;

import com.tasknotes.dto.NoteRequest;
import com.tasknotes.dto.NoteResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.Note;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.NoteRepository;
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
class NoteServiceTest {

    @Mock NoteRepository     noteRepository;
    @Mock CategoryRepository categoryRepository;

    @InjectMocks NoteService service;

    private Category stubCategory(Long id) {
        Category c = mock(Category.class);
        when(c.getId()).thenReturn(id);
        return c;
    }

    private Note stubNote(Long id, Long categoryId, String title, String content) {
        Note n = mock(Note.class);
        when(n.getId()).thenReturn(id);
        when(n.getCategory()).thenReturn(stubCategory(categoryId));
        when(n.getTitle()).thenReturn(title);
        when(n.getContent()).thenReturn(content);
        when(n.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(n.getUpdatedAt()).thenReturn(LocalDateTime.now());
        return n;
    }

    // ── findById ──────────────────────────────────────────────────────────────
    // @Test
    // void findById_returnsResponse_whenExists() {
    //     when(noteRepository.findById(1L)).thenReturn(Optional.of(stubNote(1L, 1L, "Title", "Body")));

    //     NoteResponse r = service.findById(1L);

    //     assertThat(r.id()).isEqualTo(1L);
    //     assertThat(r.title()).isEqualTo("Title");
    //     assertThat(r.content()).isEqualTo("Body");
    // }

    // @Test
    // void findById_throwsResourceNotFoundException_whenNotFound() {
    //     when(noteRepository.findById(99L)).thenReturn(Optional.empty());

    //     assertThatThrownBy(() -> service.findById(99L))
    //             .isInstanceOf(ResourceNotFoundException.class);
    // }

    // // ── findByCategory ────────────────────────────────────────────────────────
    // @Test
    // void findByCategory_returnsOrderedList() {
    //     when(categoryRepository.findById(1L)).thenReturn(Optional.of(stubCategory(1L)));
    //     when(noteRepository.findByCategoryIdOrderByCreatedAtDesc(1L))
    //             .thenReturn(List.of(
    //                     stubNote(2L, 1L, "Recent", null),
    //                     stubNote(1L, 1L, "Older",  "content")
    //             ));

    //     List<NoteResponse> result = service.findByCategory(1L);

    //     assertThat(result).hasSize(2);
    //     assertThat(result.get(0).title()).isEqualTo("Recent");
    // }

    @Test
    void findByCategory_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesAndReturnsNote() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(stubCategory(1L)));
        when(noteRepository.save(any())).thenReturn(stubNote(5L, 1L, "My Note", "Content"));

        NoteResponse r = service.create(1L, new NoteRequest("My Note", "Content"));

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.title()).isEqualTo("My Note");
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void create_throwsBusinessException_whenContentExceeds2000Chars() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(stubCategory(1L)));
        String tooLong = "x".repeat(2001);

        assertThatThrownBy(() -> service.create(1L, new NoteRequest("Title", tooLong)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2000");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void create_allowsNullContent() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(stubCategory(1L)));
        when(noteRepository.save(any())).thenReturn(stubNote(1L, 1L, "Title", null));

        NoteResponse r = service.create(1L, new NoteRequest("Title", null));

        assertThat(r.content()).isNull();
    }

    @Test
    void create_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new NoteRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────
    @Test
    void update_savesChangedFields() {
        Note existing = stubNote(1L, 1L, "Old", "Old content");
        when(noteRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(existing)).thenReturn(existing);

        service.update(1L, new NoteRequest("New Title", "New content"));

        verify(existing).setTitle("New Title");
        verify(existing).setContent("New content");
        verify(noteRepository).save(existing);
    }

    @Test
    void update_throwsBusinessException_whenContentTooLong() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(stubNote(1L, 1L, "X", null)));
        String tooLong = "y".repeat(2001);

        assertThatThrownBy(() -> service.update(1L, new NoteRequest("Title", tooLong)))
                .isInstanceOf(BusinessException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new NoteRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        when(noteRepository.findById(1L)).thenReturn(Optional.of(stubNote(1L, 1L, "X", null)));

        service.delete(1L);

        verify(noteRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(noteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(noteRepository, never()).deleteById(any());
    }
}
