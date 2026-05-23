package com.tasknotes.notes.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String uuid,
        Long categoryId,
        String title,
        String content,
        Integer position,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
