package com.tasknotes.dictionary.repository;

import com.tasknotes.dictionary.model.DictionaryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, Long> {

    // User-scoped list, alphabetical (case-insensitive via normalizedTerm)
    List<DictionaryEntry> findByUserIdOrderByNormalizedTermAsc(Long userId);

    // Duplicate guard, case-insensitive after trim (normalizedTerm already lowercased+trimmed)
    boolean existsByUserIdAndNormalizedTerm(Long userId, String normalizedTerm);
}
