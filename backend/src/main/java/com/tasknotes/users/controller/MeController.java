package com.tasknotes.users.controller;

import com.tasknotes.users.dto.UpdateProfileRequest;
import com.tasknotes.users.dto.UserResponse;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.users.repository.UserRepository;
import com.tasknotes.users.service.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;

    public MeController(SecurityHelper securityHelper, UserRepository userRepository) {
        this.securityHelper = securityHelper;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getMe() {
        AppUser user = securityHelper.currentAppUser();
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        AppUser user = securityHelper.currentAppUser();

        if (req.displayName() != null && !req.displayName().isBlank()) {
            user.setDisplayName(req.displayName().trim());
        }

        // profileImageUrl: null means no change, empty string means remove
        if (req.profileImageUrl() != null) {
            String url = req.profileImageUrl().isBlank() ? null : req.profileImageUrl().trim();
            if (url != null
                    && !url.startsWith("http://")
                    && !url.startsWith("https://")
                    && !url.startsWith("data:image/jpeg;base64,")
                    && !url.startsWith("data:image/png;base64,")) {
                return ResponseEntity.badRequest().build();
            }
            user.setProfileImageUrl(url);
        }

        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
