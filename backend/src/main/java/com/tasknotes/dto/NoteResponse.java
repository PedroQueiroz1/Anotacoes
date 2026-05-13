package com.tasknotes.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        Long categoryId,
        String title,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
