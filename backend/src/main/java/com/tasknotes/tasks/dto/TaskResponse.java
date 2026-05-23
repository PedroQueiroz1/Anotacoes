package com.tasknotes.tasks.dto;

import com.tasknotes.tasks.model.Priority;
import com.tasknotes.tasks.model.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String uuid,
        Long categoryId,
        String title,
        String description,
        LocalDate dueDate,
        Priority priority,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String tagName,
        String tagColor
) {}
