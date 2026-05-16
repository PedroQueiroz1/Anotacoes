package com.tasknotes.repository;

import com.tasknotes.model.ProgrammingConcept;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgrammingConceptRepository extends JpaRepository<ProgrammingConcept, Long> {

    Optional<ProgrammingConcept> findByNormalizedTerm(String normalizedTerm);

    List<ProgrammingConcept> findByNormalizedTermStartingWith(String prefix, Pageable pageable);
}
