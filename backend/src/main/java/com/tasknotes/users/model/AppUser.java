package com.tasknotes.users.model;

import com.tasknotes.shared.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_app_user_email",    columnNames = "email")
    }
)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true)
    private String uuid;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role = "USER";

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UuidV7Generator.generate();
    }

    public Long getId()                            { return id; }
    public String getUuid()                        { return uuid; }
    public String getUsername()                    { return username; }
    public void   setUsername(String username)     { this.username = username; }
    public String getEmail()                       { return email; }
    public void   setEmail(String email)           { this.email = email; }
    public String getDisplayName()                 { return displayName; }
    public void   setDisplayName(String name)      { this.displayName = name; }
    public String getPasswordHash()                { return passwordHash; }
    public void   setPasswordHash(String hash)     { this.passwordHash = hash; }
    public String getRole()                        { return role; }
    public void   setRole(String role)             { this.role = role; }
    public boolean isEnabled()                     { return enabled; }
    public void   setEnabled(boolean enabled)      { this.enabled = enabled; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
    public LocalDateTime getLastLoginAt()          { return lastLoginAt; }
    public void   setLastLoginAt(LocalDateTime t)  { this.lastLoginAt = t; }
}
