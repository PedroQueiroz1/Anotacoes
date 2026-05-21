package com.tasknotes.concepts.repository;

import com.tasknotes.concepts.model.ProgrammingConcept;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgrammingConceptRepository extends JpaRepository<ProgrammingConcept, Long> {

    // ── GLOBAL scope lookups ──────────────────────────────────────────────────
    Optional<ProgrammingConcept> findByNormalizedTermAndScope(String normalizedTerm, String scope);

    List<ProgrammingConcept> findByNormalizedTermStartingWithAndScope(String prefix, String scope, Pageable pageable);

    // ── USER scope lookups ────────────────────────────────────────────────────
    Optional<ProgrammingConcept> findByNormalizedTermAndOwnerUserId(String normalizedTerm, Long ownerUserId);

    // ── Legacy (kept for seedIfEmpty check) ───────────────────────────────────
    Optional<ProgrammingConcept> findByNormalizedTerm(String normalizedTerm);

    // ── Scope existence check ──────────────────────────────────────────────────
    boolean existsByScope(String scope);
}
