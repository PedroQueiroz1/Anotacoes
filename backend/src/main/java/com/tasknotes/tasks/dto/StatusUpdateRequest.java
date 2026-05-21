package com.tasknotes.tasks.dto;

import com.tasknotes.tasks.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(

        @NotNull(message = "O status é obrigatório")
        TaskStatus status
) {}
