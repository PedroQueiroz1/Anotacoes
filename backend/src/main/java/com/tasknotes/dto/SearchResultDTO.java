package com.tasknotes.dto;

public record SearchResultDTO(
        String type,
        Long id,
        String title,
        Long categoryId,
        String categoryName,
        String snippet
) {}
