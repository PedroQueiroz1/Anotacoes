package com.tasknotes.categories.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String uuid,
        String name,
        String slug,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int pendingTaskCount
) {}
