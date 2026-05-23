package com.tasknotes.tasks.dto;

import com.tasknotes.tasks.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String title,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description,

        LocalDate dueDate,

        Priority priority,

        @Size(max = 50, message = "Nome da tag deve ter no máximo 50 caracteres")
        String tagName,

        @Size(max = 9, message = "Cor da tag inválida")
        String tagColor
) {}
