package com.tasknotes.notes.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String uuid,
        Long categoryId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
