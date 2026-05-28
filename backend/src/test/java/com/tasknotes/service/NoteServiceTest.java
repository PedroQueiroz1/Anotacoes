package com.tasknotes.service;

import com.tasknotes.notes.dto.NoteRequest;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.categories.model.Category;
import com.tasknotes.notes.model.Note;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.notes.repository.NoteRepository;
import com.tasknotes.notes.repository.NoteTagRepository;
import com.tasknotes.users.service.SecurityHelper;
import com.tasknotes.notes.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock NoteRepository     noteRepository;
    @Mock NoteTagRepository  noteTagRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock SecurityHelper     securityHelper;

    @InjectMocks NoteService service;

    private static final Long OWNER_ID = 1L;

    private Category stubCategory(Long id) {
        AppUser owner = mock(AppUser.class);
        lenient().when(owner.getId()).thenReturn(OWNER_ID);
        Category c = mock(Category.class);
        lenient().when(c.getId()).thenReturn(id);
        lenient().when(c.getOwner()).thenReturn(owner);
        return c;
    }

    private Note stubNote(Long id, Long categoryId, String title, String content) {
        Category cat = stubCategory(categoryId);
        Note     n   = mock(Note.class);
        lenient().when(n.getId()).thenReturn(id);
        lenient().when(n.getUuid()).thenReturn(null);
        lenient().when(n.getCategory()).thenReturn(cat);
        lenient().when(n.getTitle()).thenReturn(title);
        lenient().when(n.getContent()).thenReturn(content);
        lenient().when(n.isPinned()).thenReturn(false);
        lenient().when(n.getPinnedAt()).thenReturn(null);
        lenient().when(n.getTags()).thenReturn(new ArrayList<>());
        lenient().when(n.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(n.getUpdatedAt()).thenReturn(LocalDateTime.now());
        return n;
    }

    @BeforeEach
    void setup() {
        lenient().when(securityHelper.currentUserId()).thenReturn(OWNER_ID);
    }

    // ── findByCategory ────────────────────────────────────────────────────────
    @Test
    void findByCategory_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findByIdWithOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCategory(99L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────
    @Test
    void create_savesAndReturnsNote() {
        Category cat  = stubCategory(1L);
        Note     note = stubNote(5L, 1L, "My Note", "Content");
        when(categoryRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(cat));
        when(noteRepository.save(any())).thenReturn(note);
        when(noteRepository.findByIdWithTags(5L)).thenReturn(Optional.of(note));

        NoteResponse r = service.create(1L, new NoteRequest("My Note", "Content", null, null));

        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.title()).isEqualTo("My Note");
        verify(noteRepository).save(any(Note.class));
    }

    @Test
    void create_throwsBusinessException_whenContentExceeds2000Chars() {
        Category cat = stubCategory(1L);
        when(categoryRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(cat));
        String tooLong = "x".repeat(2001);

        assertThatThrownBy(() -> service.create(1L, new NoteRequest("Title", tooLong, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2000");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void create_allowsNullContent() {
        Category cat  = stubCategory(1L);
        Note     note = stubNote(1L, 1L, "Title", null);
        when(categoryRepository.findByIdWithOwner(1L)).thenReturn(Optional.of(cat));
        when(noteRepository.save(any())).thenReturn(note);
        when(noteRepository.findByIdWithTags(1L)).thenReturn(Optional.of(note));

        NoteResponse r = service.create(1L, new NoteRequest("Title", null, null, null));

        assertThat(r.content()).isNull();
    }

    @Test
    void create_throwsResourceNotFoundException_whenCategoryMissing() {
        when(categoryRepository.findByIdWithOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, new NoteRequest("X", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── update ────────────────────────────────────────────────────────────────
    @Test
    void update_savesChangedFields() {
        Note existing = stubNote(1L, 1L, "Old", "Old content");
        when(noteRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(existing));
        when(noteRepository.save(existing)).thenReturn(existing);
        when(noteRepository.findByIdWithTags(1L)).thenReturn(Optional.of(existing));

        service.update(1L, new NoteRequest("New Title", "New content", null, null));

        verify(existing).setTitle("New Title");
        verify(existing).setContent("New content");
        verify(noteRepository).save(existing);
    }

    @Test
    void update_throwsBusinessException_whenContentTooLong() {
        Note note = stubNote(1L, 1L, "X", null);
        when(noteRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(note));
        String tooLong = "y".repeat(2001);

        assertThatThrownBy(() -> service.update(1L, new NoteRequest("Title", tooLong, null, null)))
                .isInstanceOf(BusinessException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void update_throwsResourceNotFoundException_whenNotFound() {
        when(noteRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new NoteRequest("X", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────
    @Test
    void delete_callsDeleteById_whenExists() {
        Note note = stubNote(1L, 1L, "X", null);
        when(noteRepository.findByIdWithCategoryAndOwner(1L)).thenReturn(Optional.of(note));

        service.delete(1L);

        verify(noteRepository).deleteById(1L);
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        when(noteRepository.findByIdWithCategoryAndOwner(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(noteRepository, never()).deleteById(any());
    }
}
