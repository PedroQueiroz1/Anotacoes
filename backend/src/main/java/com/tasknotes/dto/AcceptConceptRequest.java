package com.tasknotes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptConceptRequest(
        @NotBlank @Size(max = 80) String term,
        @NotBlank @Size(max = 2000) String summary,
        String source,
        String sourceUrl
) {}
