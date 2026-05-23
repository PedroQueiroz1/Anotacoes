package com.tasknotes.notes.service;

import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.notes.dto.NoteRequest;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.categories.model.Category;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.notes.model.Note;
import com.tasknotes.notes.repository.NoteRepository;
import com.tasknotes.shared.util.NoteCursorCodec;
import com.tasknotes.users.service.SecurityHelper;
import com.tasknotes.shared.util.UuidV7Generator;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NoteService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int NOTE_LIMIT = 10;

    private final NoteRepository noteRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityHelper securityHelper;

    public NoteService(NoteRepository noteRepository,
                       CategoryRepository categoryRepository,
                       SecurityHelper securityHelper) {
        this.noteRepository = noteRepository;
        this.categoryRepository = categoryRepository;
        this.securityHelper = securityHelper;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<NoteResponse> findByCategory(Long categoryId, String cursor) {
        findCategoryWithOwnership(categoryId);
        int fetch = NOTE_LIMIT + 1;
        var pageable = PageRequest.of(0, fetch);
        List<Note> raw;

        if (cursor == null) {
            raw = noteRepository.findByCategoryFirstPage(categoryId, pageable);
        } else {
            NoteCursorCodec.Payload p = NoteCursorCodec.decode(cursor);
            if (p == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor de paginação inválido.");
            if (p.zone() == 0 && p.position() != null) {
                raw = noteRepository.findByCategoryAfterPositionedZone(categoryId, p.position(), p.id(), pageable);
            } else {
                if (p.createdAt() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cursor de paginação inválido.");
                raw = noteRepository.findByCategoryAfterUnpositionedZone(categoryId, p.createdAt(), p.id(), pageable);
            }
        }

        boolean hasNext = raw.size() > NOTE_LIMIT;
        List<Note> page = hasNext ? raw.subList(0, NOTE_LIMIT) : raw;
        String nextCursor = null;
        if (hasNext) {
            Note last = page.get(page.size() - 1);
            nextCursor = last.getPosition() != null
                    ? NoteCursorCodec.encodePositioned(last.getPosition(), last.getId())
                    : NoteCursorCodec.encodeUnpositioned(last.getCreatedAt(), last.getId());
        }

        long totalCount = noteRepository.countByCategoryId(categoryId);
        return new CursorPageResponse<>(page.stream().map(this::toResponse).toList(), nextCursor, hasNext, NOTE_LIMIT, totalCount);
    }

    public NoteResponse create(Long categoryId, NoteRequest request) {
        Category category = findCategoryWithOwnership(categoryId);
        validateContent(request.content());

        Note note = new Note();
        note.setCategory(category);
        note.setTitle(request.title().trim());
        note.setContent(request.content());
        return toResponse(noteRepository.save(note));
    }

    public NoteResponse update(Long id, NoteRequest request) {
        Note note = findOrThrow(id);
        validateContent(request.content());

        note.setTitle(request.title().trim());
        note.setContent(request.content());
        return toResponse(noteRepository.save(note));
    }

    public void delete(Long id) {
        findOrThrow(id);
        noteRepository.deleteById(id);
    }

    @Transactional
    public void reorder(Long categoryId, List<Long> ids) {
        findCategoryWithOwnership(categoryId);
        for (int i = 0; i < ids.size(); i++) {
            noteRepository.updatePosition(ids.get(i), i);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillUuids() {
        for (Note n : noteRepository.findAll()) {
            if (n.getUuid() == null) {
                n.setUuid(UuidV7Generator.generate());
                noteRepository.save(n);
            }
        }
    }

    private void validateContent(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("Conteúdo excede o limite de " + MAX_CONTENT_LENGTH + " caracteres.");
        }
    }

    private Category findCategoryWithOwnership(Long categoryId) {
        Long ownerId = securityHelper.currentUserId();
        Category cat = categoryRepository.findByIdWithOwner(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoryId));
        if (cat.getOwner() == null || !ownerId.equals(cat.getOwner().getId())) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + categoryId);
        }
        return cat;
    }

    private Note findOrThrow(Long id) {
        Long ownerId = securityHelper.currentUserId();
        Note note = noteRepository.findByIdWithCategoryAndOwner(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anotação não encontrada: " + id));
        Category cat = note.getCategory();
        if (cat.getOwner() == null || !ownerId.equals(cat.getOwner().getId())) {
            throw new ResourceNotFoundException("Anotação não encontrada: " + id);
        }
        return note;
    }

    private NoteResponse toResponse(Note n) {
        return new NoteResponse(
                n.getId(),
                n.getUuid(),
                n.getCategory().getId(),
                n.getTitle(),
                n.getContent(),
                n.getPosition(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
