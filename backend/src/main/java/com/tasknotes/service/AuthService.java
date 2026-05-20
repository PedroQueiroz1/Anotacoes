package com.tasknotes.service;

import com.tasknotes.dto.CreateUserRequest;
import com.tasknotes.dto.LoginRequest;
import com.tasknotes.dto.LoginResponse;
import com.tasknotes.dto.UserResponse;
import com.tasknotes.model.AppUser;
import com.tasknotes.model.RefreshToken;
import com.tasknotes.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    static final String REFRESH_COOKIE = "refresh_token";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshTokenDays;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.refresh-token-expiration-days}") long refreshTokenDays,
            @Value("${app.cookie.secure}") boolean cookieSecure,
            @Value("${app.cookie.same-site}") String cookieSameSite) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenDays = refreshTokenDays;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @Transactional
    public LoginResponse login(LoginRequest req, HttpServletRequest httpReq, HttpServletResponse httpRes) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        AppUser user = userRepository.findByUsername(auth.getName())
                .orElseThrow();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generate(user.getUsername(), user.getRole());

        if (req.rememberMe()) {
            String rawRefresh = refreshTokenService.createToken(user, userAgent(httpReq), ipHash(httpReq));
            addRefreshCookie(httpRes, rawRefresh);
        }

        return new LoginResponse(accessToken, UserResponse.from(user));
    }

    @Transactional
    public LoginResponse refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        String rawToken = extractRefreshCookie(httpReq)
                .orElseThrow(() -> new IllegalArgumentException("refresh_token cookie missing"));

        RefreshToken token = refreshTokenService.findValid(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("invalid or expired refresh token"));

        AppUser user = token.getUser();
        String newRaw = refreshTokenService.rotate(token, userAgent(httpReq), ipHash(httpReq));
        addRefreshCookie(httpRes, newRaw);

        String accessToken = jwtService.generate(user.getUsername(), user.getRole());
        return new LoginResponse(accessToken, UserResponse.from(user));
    }

    @Transactional
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        extractRefreshCookie(httpReq).ifPresent(refreshTokenService::revoke);
        clearRefreshCookie(httpRes);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("email already registered");
        }

        AppUser user = new AppUser();
        user.setDisplayName(req.displayName());
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole("USER");
        user.setEnabled(req.enabled());
        userRepository.save(user);

        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse toggleEnabled(String uuid) {
        AppUser user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + uuid));
        user.setEnabled(!user.isEnabled());
        if (!user.isEnabled()) {
            refreshTokenService.revokeAll(user.getId());
        }
        userRepository.save(user);
        return UserResponse.from(user);
    }

    private void addRefreshCookie(HttpServletResponse res, String rawToken) {
        int maxAge = (int) (refreshTokenDays * 24 * 60 * 60);
        String header = REFRESH_COOKIE + "=" + rawToken
                + "; HttpOnly; Path=/api/auth; Max-Age=" + maxAge
                + (cookieSecure ? "; Secure" : "")
                + "; SameSite=" + cookieSameSite;
        res.addHeader("Set-Cookie", header);
    }

    private void clearRefreshCookie(HttpServletResponse res) {
        String header = REFRESH_COOKIE + "=; HttpOnly; Path=/api/auth; Max-Age=0"
                + (cookieSecure ? "; Secure" : "")
                + "; SameSite=" + cookieSameSite;
        res.addHeader("Set-Cookie", header);
    }

    private Optional<String> extractRefreshCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return Optional.empty();
        return Arrays.stream(req.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private static String userAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }

    private static String ipHash(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
        return RefreshTokenService.sha256(ip.split(",")[0].trim());
    }
}
