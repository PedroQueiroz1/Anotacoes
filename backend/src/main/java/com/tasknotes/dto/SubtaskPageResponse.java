package com.tasknotes.dto;

import java.util.List;

public record SubtaskPageResponse(
        List<SubtaskResponse> items,
        String nextCursor,
        boolean hasNext,
        int limit,
        long totalCount,
        long completedCount
) {}
