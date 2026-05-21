package com.tasknotes.tasks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubtaskRequest(

        @NotBlank(message = "O texto é obrigatório")
        @Size(max = 200, message = "O texto deve ter no máximo 200 caracteres")
        String text
) {}
