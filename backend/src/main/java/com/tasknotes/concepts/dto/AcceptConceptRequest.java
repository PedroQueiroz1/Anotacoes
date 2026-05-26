package com.tasknotes.concepts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptConceptRequest(
        @NotBlank @Size(max = 120) String term,
        @NotBlank @Size(max = 2000) String summary,
        String source,
        String sourceUrl
) {}
