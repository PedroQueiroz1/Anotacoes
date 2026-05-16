package com.tasknotes.dto;

import com.tasknotes.model.Priority;
import jakarta.validation.constraints.NotNull;

public record UpdatePriorityRequest(

        @NotNull(message = "A prioridade é obrigatória")
        Priority priority
) {}
