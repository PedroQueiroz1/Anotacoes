package com.tasknotes.dto;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {}
