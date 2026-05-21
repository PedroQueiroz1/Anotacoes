package com.tasknotes.search.service;

import com.tasknotes.search.dto.SearchResultDTO;
import com.tasknotes.categories.repository.CategoryRepository;
import com.tasknotes.notes.repository.NoteRepository;
import com.tasknotes.tasks.repository.SubtaskRepository;
import com.tasknotes.tasks.repository.TaskRepository;
import com.tasknotes.users.service.SecurityHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SearchService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final NoteRepository noteRepository;
    private final SecurityHelper securityHelper;

    public SearchService(CategoryRepository categoryRepository,
                         TaskRepository taskRepository,
                         SubtaskRepository subtaskRepository,
                         NoteRepository noteRepository,
                         SecurityHelper securityHelper) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.noteRepository = noteRepository;
        this.securityHelper = securityHelper;
    }

    public List<SearchResultDTO> search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) return List.of();

        Long ownerId = securityHelper.currentUserId();
        String pattern = "%" + trimmed + "%";
        List<SearchResultDTO> results = new ArrayList<>();
        Set<Long> addedTaskIds = new HashSet<>();

        categoryRepository.findByNameContainingIgnoreCaseAndOwnerId(trimmed, ownerId)
                .forEach(cat -> results.add(new SearchResultDTO(
                        "CATEGORY", cat.getId(), cat.getName(), null, null, null)));

        taskRepository.searchByTitleOrDescriptionAndOwnerId(pattern, ownerId)
                .forEach(task -> {
                    addedTaskIds.add(task.getId());
                    results.add(new SearchResultDTO(
                            "TASK", task.getId(), task.getTitle(),
                            task.getCategory().getId(), task.getCategory().getName(),
                            truncate(task.getDescription(), 120)));
                });

        subtaskRepository.searchByTextAndOwnerId(pattern, ownerId)
                .forEach(sub -> {
                    var parent = sub.getTask();
                    if (addedTaskIds.add(parent.getId())) {
                        results.add(new SearchResultDTO(
                                "TASK", parent.getId(), parent.getTitle(),
                                parent.getCategory().getId(), parent.getCategory().getName(),
                                truncate(sub.getText(), 120)));
                    }
                });

        noteRepository.searchByTitleOrContentAndOwnerId(pattern, ownerId)
                .forEach(note -> results.add(new SearchResultDTO(
                        "NOTE", note.getId(), note.getTitle(),
                        note.getCategory().getId(), note.getCategory().getName(),
                        truncate(note.getContent(), 120))));

        return results;
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "…";
    }
}
