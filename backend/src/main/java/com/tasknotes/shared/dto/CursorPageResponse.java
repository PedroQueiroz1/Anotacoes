package com.tasknotes.shared.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext,
        int limit,
        Long totalCount
) {}
