package com.tasknotes.dictionary.dto;

public record DictionaryEntryResponse(
        String uuid,
        String term,
        String definition
) {}
