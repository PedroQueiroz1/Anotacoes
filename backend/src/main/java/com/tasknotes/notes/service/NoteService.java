package com.tasknotes.notes.service;

import com.tasknotes.shared.dto.CursorPageResponse;
import com.tasknotes.notes.dto.NoteRequest;
import com.tasknotes.notes.dto.NoteResponse;
import com.tasknotes.notes.dto.NoteTagResponse;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.categories.model.Category;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.notes.model.Note;
import com.tasknotes.notes.model.NoteTag;
import com.tasknotes.notes.repository.NoteRepository;
import com.tasknotes.notes.repository.NoteTagRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NoteService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int NOTE_LIMIT = 20;

    private final NoteRepository noteRepository;
    private final NoteTagRepository noteTagRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityHelper securityHelper;

    public NoteService(NoteRepository noteRepository,
                       NoteTagRepository noteTagRepository,
                       CategoryRepository categoryRepository,
                       SecurityHelper securityHelper) {
        this.noteRepository = noteRepository;
        this.noteTagRepository = noteTagRepository;
        this.categoryRepository = categoryRepository;
        this.securityHelper = securityHelper;
    }

    // ── findByCategory with filters ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public CursorPageResponse<NoteResponse> findByCategory(Long categoryId, String cursor) {
        return findByCategory(categoryId, cursor, null, "recent", null, null);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<NoteResponse> findByCategory(Long categoryId, String cursor,
                                                            String query, String sort, Long tagId,
                                                            Boolean pinnedOnly) {
        findCategoryWithOwnership(categoryId);

        String likeQuery = (query != null && !query.isBlank()) ? "%" + query.trim() + "%" : null;
        String effectiveSort = (sort != null) ? sort : "recent";
        // Convert Boolean to Integer for MySQL compatibility (NULL=all, 1=pinned, 0=unpinned)
        Integer pinnedOnlyInt = pinnedOnly == null ? null : (pinnedOnly ? 1 : 0);

        long cursorId = 0;
        if (cursor != null) {
            try {
                cursorId = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                NoteCursorCodec.Payload p = NoteCursorCodec.decode(cursor);
                if (p != null) cursorId = p.id() != null ? p.id() : 0;
            }
        }

        List<Note> raw = loadPage(categoryId, likeQuery, effectiveSort, tagId, pinnedOnlyInt, cursorId);

        boolean hasNext = raw.size() > NOTE_LIMIT;
        List<Note> page = hasNext ? raw.subList(0, NOTE_LIMIT) : raw;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            nextCursor = String.valueOf(page.get(page.size() - 1).getId());
        }

        long totalCount = noteRepository.countByCategoryFiltered(categoryId, likeQuery, tagId, pinnedOnlyInt);

        List<Note> withTags = loadTagsForNotes(page);

        return new CursorPageResponse<>(withTags.stream().map(this::toResponse).toList(),
                nextCursor, hasNext, NOTE_LIMIT, totalCount);
    }

    private List<Note> loadPage(Long categoryId, String likeQuery, String sort,
                                 Long tagId, Integer pinnedOnly, long cursorId) {
        int offset = (int) cursorId;
        var offsetPageable = PageRequest.of(0, NOTE_LIMIT + 1 + offset);

        List<Note> all = switch (sort) {
            case "oldest"     -> noteRepository.findByCategoryFilteredOldest(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
            case "title_asc"  -> noteRepository.findByCategoryFilteredTitleAsc(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
            case "title_desc" -> noteRepository.findByCategoryFilteredTitleDesc(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
            case "pinned"     -> noteRepository.findByCategoryFilteredPinned(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
            case "manual"     -> noteRepository.findByCategoryFilteredManual(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
            default           -> noteRepository.findByCategoryFilteredRecent(categoryId, likeQuery, tagId, pinnedOnly, offsetPageable);
        };

        if (offset > 0 && offset < all.size()) {
            return all.subList(offset, all.size());
        } else if (offset >= all.size()) {
            return new ArrayList<>();
        }
        return all;
    }

    private List<Note> loadTagsForNotes(List<Note> notes) {
        // Load tags individually - acceptable for typical page sizes (20 items)
        List<Note> result = new ArrayList<>();
        for (Note n : notes) {
            Note withTags = noteRepository.findByIdWithTags(n.getId()).orElse(n);
            result.add(withTags);
        }
        return result;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    @Transactional
    public NoteResponse create(Long categoryId, NoteRequest request) {
        Category category = findCategoryWithOwnership(categoryId);
        validateContent(request.content());

        Note note = new Note();
        note.setCategory(category);
        note.setTitle(request.title().trim());
        note.setContent(request.content());

        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            Long ownerId = securityHelper.currentUserId();
            List<NoteTag> tags = resolveTagsForOwner(request.tagIds(), ownerId);
            note.setTags(tags);
        }

        Note saved = noteRepository.save(note);
        return toResponse(noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    @Transactional
    public NoteResponse update(Long id, NoteRequest request) {
        Note note = findOrThrow(id);
        validateContent(request.content());

        note.setTitle(request.title().trim());
        note.setContent(request.content());

        if (request.tagIds() != null) {
            Long ownerId = securityHelper.currentUserId();
            List<NoteTag> tags = resolveTagsForOwner(request.tagIds(), ownerId);
            note.setTags(tags);
        }

        Note saved = noteRepository.save(note);
        return toResponse(noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        noteRepository.deleteById(id);
    }

    @Transactional
    public void reorder(Long categoryId, List<Long> ids, int offset) {
        findCategoryWithOwnership(categoryId);
        for (int i = 0; i < ids.size(); i++) {
            noteRepository.updatePosition(ids.get(i), offset + i);
        }
    }

    // ── Pin ───────────────────────────────────────────────────────────────────
    @Transactional
    public NoteResponse togglePin(Long id) {
        Note note = findOrThrow(id);
        boolean newPinned = !note.isPinned();
        note.setPinned(newPinned);
        note.setPinnedAt(newPinned ? LocalDateTime.now() : null);
        Note saved = noteRepository.save(note);
        return toResponse(noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    // ── Move to top ───────────────────────────────────────────────────────────
    @Transactional
    public NoteResponse moveToTop(Long id) {
        Note note = findOrThrow(id);
        Integer minPos = noteRepository.findMinPosition(note.getCategory().getId());
        int newPos = (minPos != null) ? minPos - 1 : 0;
        note.setPosition(newPos);
        Note saved = noteRepository.save(note);
        return toResponse(noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    // ── Move to bottom ────────────────────────────────────────────────────────
    @Transactional
    public NoteResponse moveToBottom(Long id) {
        Note note = findOrThrow(id);
        Integer maxPos = noteRepository.findMaxPosition(note.getCategory().getId());
        int newPos = (maxPos != null) ? maxPos + 1 : 0;
        note.setPosition(newPos);
        Note saved = noteRepository.save(note);
        return toResponse(noteRepository.findByIdWithTags(saved.getId()).orElse(saved));
    }

    // ── Move to position ──────────────────────────────────────────────────────
    @Transactional
    public NoteResponse moveToPosition(Long id, int targetPosition) {
        Note note = findOrThrow(id);
        Long categoryId = note.getCategory().getId();

        List<Note> all = noteRepository.findByCategoryIdOrdered(categoryId);
        // Remove the note being moved
        all = new ArrayList<>(all.stream().filter(n -> !n.getId().equals(id)).toList());

        // Clamp target position (1-based)
        int target = Math.max(1, Math.min(targetPosition, all.size() + 1)) - 1; // 0-based index

        all.add(target, note);

        // Re-assign positions
        for (int i = 0; i < all.size(); i++) {
            noteRepository.updatePosition(all.get(i).getId(), i);
        }

        note.setPosition(target);
        return toResponse(noteRepository.findByIdWithTags(id).orElse(note));
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

    // ── Helpers ───────────────────────────────────────────────────────────────
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

    private List<NoteTag> resolveTagsForOwner(List<Long> tagIds, Long ownerId) {
        List<NoteTag> tags = new ArrayList<>();
        for (Long tagId : tagIds) {
            noteTagRepository.findById(tagId).ifPresent(tag -> {
                if (ownerId.equals(tag.getOwner().getId())) {
                    tags.add(tag);
                }
            });
        }
        return tags;
    }

    NoteResponse toResponse(Note n) {
        List<NoteTagResponse> tagResponses = n.getTags() != null
                ? n.getTags().stream().map(t -> new NoteTagResponse(t.getId(), t.getName(), t.getColor())).toList()
                : List.of();
        return new NoteResponse(
                n.getId(),
                n.getUuid(),
                n.getCategory().getId(),
                n.getTitle(),
                n.getContent(),
                n.getPosition(),
                n.isPinned(),
                n.getPinnedAt(),
                tagResponses,
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
