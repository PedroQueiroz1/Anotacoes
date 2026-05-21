package com.tasknotes.users.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_token", indexes = {
    @Index(name = "idx_refresh_token_hash",    columnList = "token_hash"),
    @Index(name = "idx_refresh_token_user_id", columnList = "user_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    public Long getId()                           { return id; }
    public String getTokenHash()                  { return tokenHash; }
    public void   setTokenHash(String tokenHash)  { this.tokenHash = tokenHash; }
    public AppUser getUser()                      { return user; }
    public void   setUser(AppUser user)           { this.user = user; }
    public LocalDateTime getExpiresAt()           { return expiresAt; }
    public void   setExpiresAt(LocalDateTime t)   { this.expiresAt = t; }
    public boolean isRevoked()                    { return revoked; }
    public void   setRevoked(boolean revoked)     { this.revoked = revoked; }
    public LocalDateTime getCreatedAt()           { return createdAt; }
    public LocalDateTime getRevokedAt()           { return revokedAt; }
    public void   setRevokedAt(LocalDateTime t)   { this.revokedAt = t; }
    public String getUserAgent()                  { return userAgent; }
    public void   setUserAgent(String ua)         { this.userAgent = ua; }
    public String getIpHash()                     { return ipHash; }
    public void   setIpHash(String ipHash)        { this.ipHash = ipHash; }
}
