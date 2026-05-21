package com.tasknotes.tasks.dto;

import com.tasknotes.tasks.model.Priority;
import jakarta.validation.constraints.NotNull;

public record UpdatePriorityRequest(

        @NotNull(message = "A prioridade é obrigatória")
        Priority priority
) {}
