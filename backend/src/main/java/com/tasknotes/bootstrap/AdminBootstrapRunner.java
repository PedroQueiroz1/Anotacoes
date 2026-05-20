package com.tasknotes.bootstrap;

import com.tasknotes.model.AppUser;
import com.tasknotes.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrapRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminDisplayName;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword,
            @Value("${app.admin.display-name}") String adminDisplayName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminDisplayName = adminDisplayName;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void run() {
        if (adminUsername.isBlank() || adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("admin_bootstrap_skipped reason=env_vars_not_set");
            return;
        }

        if (userRepository.existsByUsername(adminUsername)) {
            log.info("admin_bootstrap_skipped reason=admin_already_exists username={}", adminUsername);
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setDisplayName(adminDisplayName.isBlank() ? "Admin" : adminDisplayName);
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        userRepository.save(admin);

        log.info("admin_bootstrap_created username={}", adminUsername);
    }
}
