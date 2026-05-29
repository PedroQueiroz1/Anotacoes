package com.tasknotes.notes.dto;

import jakarta.validation.constraints.Size;

public record SmartFormatApplyRequest(
        @Size(max = 1000)   String formattedTitleHtml,
        @Size(max = 50000)  String formattedContentHtml
) {}
