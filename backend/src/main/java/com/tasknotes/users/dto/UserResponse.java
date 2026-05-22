package com.tasknotes.users.dto;

import com.tasknotes.users.model.AppUser;

import java.time.LocalDateTime;

public record UserResponse(
        String uuid,
        String username,
        String email,
        String displayName,
        String role,
        boolean enabled,
        LocalDateTime createdAt
) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getUuid(), u.getUsername(), u.getEmail(),
                u.getDisplayName(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
