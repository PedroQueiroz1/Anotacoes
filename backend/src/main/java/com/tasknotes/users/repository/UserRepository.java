package com.tasknotes.users.repository;

import com.tasknotes.users.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findByUuid(String uuid);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
