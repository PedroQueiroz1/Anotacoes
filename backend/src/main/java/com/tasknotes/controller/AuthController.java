package com.tasknotes.controller;

import com.tasknotes.dto.CreateUserRequest;
import com.tasknotes.dto.LoginRequest;
import com.tasknotes.dto.LoginResponse;
import com.tasknotes.dto.UserResponse;
import com.tasknotes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                               HttpServletRequest httpReq,
                                               HttpServletResponse httpRes) {
        return ResponseEntity.ok(authService.login(req, httpReq, httpRes));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        try {
            return ResponseEntity.ok(authService.refresh(httpReq, httpRes));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        authService.logout(httpReq, httpRes);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest req) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }

    @PatchMapping("/admin/users/{uuid}/toggle-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> toggleEnabled(@PathVariable String uuid) {
        try {
            return ResponseEntity.ok(authService.toggleEnabled(uuid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
