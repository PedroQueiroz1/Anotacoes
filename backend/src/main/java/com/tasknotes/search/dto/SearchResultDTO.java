package com.tasknotes.search.dto;

public record SearchResultDTO(
        String type,
        Long id,
        String title,
        Long categoryId,
        String categoryName,
        String categorySlug,
        String snippet
) {}
