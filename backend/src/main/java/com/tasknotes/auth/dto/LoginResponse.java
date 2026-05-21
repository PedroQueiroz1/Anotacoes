package com.tasknotes.auth.dto;

import com.tasknotes.users.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {}
