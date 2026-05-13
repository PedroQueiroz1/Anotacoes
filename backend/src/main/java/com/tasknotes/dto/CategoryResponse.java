package com.tasknotes.dto;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int pendingTaskCount   // populado a partir da Etapa 3 (CRUD de Tarefas)
) {}
