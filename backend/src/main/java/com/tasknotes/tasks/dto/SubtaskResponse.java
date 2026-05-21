package com.tasknotes.tasks.dto;

import java.time.LocalDateTime;

public record SubtaskResponse(
        Long id,
        String uuid,
        Long taskId,
        String text,
        boolean done,
        LocalDateTime createdAt
) {}
