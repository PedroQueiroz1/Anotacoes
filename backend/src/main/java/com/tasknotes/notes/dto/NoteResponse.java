package com.tasknotes.notes.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NoteResponse(
        Long id,
        String uuid,
        Long categoryId,
        String title,
        String content,
        Integer position,
        boolean pinned,
        LocalDateTime pinnedAt,
        List<NoteTagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
