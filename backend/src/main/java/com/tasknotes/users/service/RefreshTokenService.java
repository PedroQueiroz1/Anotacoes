package com.tasknotes.users.service;

import com.tasknotes.users.model.AppUser;
import com.tasknotes.users.model.RefreshToken;
import com.tasknotes.users.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public String createToken(AppUser user, String userAgent, String ipHash) {
        byte[] raw = new byte[32];
        secureRandom.nextBytes(raw);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(sha256(rawToken));
        entity.setUser(user);
        entity.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        entity.setUserAgent(truncate(userAgent, 500));
        entity.setIpHash(ipHash);
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValid(String rawToken) {
        return refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    public String rotate(RefreshToken old, String userAgent, String ipHash) {
        old.setRevoked(true);
        old.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(old);
        return createToken(old.getUser(), userAgent, ipHash);
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            t.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void purgeExpired() {
        refreshTokenRepository.deleteExpired(LocalDateTime.now());
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
