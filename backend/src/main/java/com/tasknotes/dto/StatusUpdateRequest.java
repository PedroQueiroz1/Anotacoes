package com.tasknotes.dto;

import com.tasknotes.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(

        @NotNull(message = "O status é obrigatório")
        TaskStatus status
) {}
