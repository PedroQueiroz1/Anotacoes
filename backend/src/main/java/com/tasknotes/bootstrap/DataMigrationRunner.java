package com.tasknotes.bootstrap;

import com.tasknotes.model.AppUser;
import com.tasknotes.model.Category;
import com.tasknotes.repository.CategoryRepository;
import com.tasknotes.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(10)
public class DataMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final String adminUsername;

    public DataMigrationRunner(CategoryRepository categoryRepository,
                               UserRepository userRepository,
                               @Value("${app.admin.username}") String adminUsername) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateOrphanedData() {
        if (adminUsername.isBlank()) {
            log.warn("data_migration_skipped reason=admin_username_not_set");
            return;
        }

        AppUser admin = userRepository.findByUsername(adminUsername).orElse(null);
        if (admin == null) {
            log.warn("data_migration_skipped reason=admin_user_not_found username={}", adminUsername);
            return;
        }

        List<Category> orphaned = categoryRepository.findAll().stream()
                .filter(c -> c.getOwner() == null)
                .toList();

        if (orphaned.isEmpty()) {
            log.info("data_migration_skipped reason=no_orphaned_categories");
            return;
        }

        for (Category c : orphaned) {
            c.setOwner(admin);
            categoryRepository.save(c);
        }

        log.info("data_migration_completed assigned_categories={} admin={}", orphaned.size(), adminUsername);
    }
}
