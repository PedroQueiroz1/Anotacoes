package com.tasknotes.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NoteRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 2000) String content,
        List<Long> tagIds
) {}
