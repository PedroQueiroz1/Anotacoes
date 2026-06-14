package com.tasknotes.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DictionaryEntryRequest(
        @NotBlank @Size(max = 100) String term,
        @NotBlank @Size(max = 2000) String definition
) {}
