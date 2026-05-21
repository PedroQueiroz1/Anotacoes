package com.tasknotes.categories.repository;

import com.tasknotes.categories.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ── System-level (no user filter — used by backfill runners) ─────────────
    @Query("SELECT c FROM Category c ORDER BY " +
           "CASE WHEN c.position IS NULL THEN 1 ELSE 0 END ASC, " +
           "c.position ASC, c.createdAt ASC")
    List<Category> findAllOrdered();

    // ── User-scoped listing ───────────────────────────────────────────────────
    @Query("SELECT c FROM Category c WHERE c.owner.id = :ownerId ORDER BY " +
           "CASE WHEN c.position IS NULL THEN 1 ELSE 0 END ASC, " +
           "c.position ASC, c.createdAt ASC")
    List<Category> findAllByOwnerIdOrdered(@Param("ownerId") Long ownerId);

    // ── User-scoped slug / search ─────────────────────────────────────────────
    Optional<Category> findBySlugAndOwnerId(String slug, Long ownerId);

    boolean existsBySlugAndOwnerId(String slug, Long ownerId);

    boolean existsBySlugAndIdNotAndOwnerId(String slug, Long id, Long ownerId);

    List<Category> findByNameContainingIgnoreCaseAndOwnerId(String name, Long ownerId);

    long countByOwnerId(Long ownerId);

    // ── Legacy (kept for compatibility with CategoryExportService) ─────────────
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findByNameContainingIgnoreCase(String name);

    // ── Ownership-safe lookup (loads owner in the same query) ────────────────
    @Query("SELECT c FROM Category c JOIN FETCH c.owner WHERE c.id = :id")
    Optional<Category> findByIdWithOwner(@Param("id") Long id);

    // ── Reorder ───────────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Category c SET c.position = :position WHERE c.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);
}
