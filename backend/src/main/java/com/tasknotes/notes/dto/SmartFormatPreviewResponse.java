package com.tasknotes.notes.dto;

import java.util.List;

public record SmartFormatPreviewResponse(
        Long noteId,
        String formattedTitleHtml,
        String formattedContentHtml,
        boolean plainTextPreserved,
        List<String> operationsSummary,
        List<String> warnings
) {}
