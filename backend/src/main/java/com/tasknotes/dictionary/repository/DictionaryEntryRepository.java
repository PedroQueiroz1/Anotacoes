package com.tasknotes.dictionary.repository;

import com.tasknotes.dictionary.model.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    // User-scoped list, alphabetical (case-insensitive via normalizedTerm)
    List<DictionaryEntry> findByUserIdOrderByNormalizedTermAsc(Long userId);

    // Ownership-safe lookup by public identifier
    Optional<DictionaryEntry> findByUuidAndUserId(String uuid, Long userId);

    // Duplicate guard, case-insensitive after trim (normalizedTerm already lowercased+trimmed)
    boolean existsByUserIdAndNormalizedTerm(Long userId, String normalizedTerm);

    // Duplicate guard for updates: excludes the entry being edited
    boolean existsByUserIdAndNormalizedTermAndUuidNot(Long userId, String normalizedTerm, String uuid);
}
