package com.tasknotes.service;

import com.tasknotes.dto.NoteRequest;
import com.tasknotes.dto.NoteResponse;
import com.tasknotes.exception.BusinessException;
import com.tasknotes.exception.ResourceNotFoundException;
import com.tasknotes.model.Category;
import com.tasknotes.model.Note;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.NoteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final NoteRepository noteRepository;
    private final CategoryRepository categoryRepository;

    public NoteService(NoteRepository noteRepository, CategoryRepository categoryRepository) {
        this.noteRepository = noteRepository;
        this.categoryRepository = categoryRepository;
    }

    public NoteResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<NoteResponse> findByCategory(Long categoryId) {
        findCategoryOrThrow(categoryId);
        return noteRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId)
                .stream().map(this::toResponse).toList();
    }

    public NoteResponse create(Long categoryId, NoteRequest request) {
        Category category = findCategoryOrThrow(categoryId);
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

    private void validateContent(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("Conteúdo excede o limite de " + MAX_CONTENT_LENGTH + " caracteres.");
        }
    }

    private Category findCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + categoryId));
    }

    private Note findOrThrow(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Anotação não encontrada: " + id));
    }

    private NoteResponse toResponse(Note n) {
        return new NoteResponse(
                n.getId(),
                n.getCategory().getId(),
                n.getTitle(),
                n.getContent(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
