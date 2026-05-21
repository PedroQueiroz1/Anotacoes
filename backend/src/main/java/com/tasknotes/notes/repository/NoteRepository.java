package com.tasknotes.notes.repository;

import com.tasknotes.notes.model.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // ── Legacy ordering (reorder) ─────────────────────────────────────────────
    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId ORDER BY " +
                   "CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, created_at DESC",
           nativeQuery = true)
    List<Note> findByCategoryIdOrdered(@Param("categoryId") Long categoryId);

    // ── Cursor pagination — first page ────────────────────────────────────────
    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Note> findByCategoryFirstPage(@Param("categoryId") Long categoryId, Pageable pageable);

    // ── Cursor pagination — after cursor ──────────────────────────────────────
    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId " +
                   "AND (created_at < :createdAt OR (created_at = :createdAt AND id < :cursorId)) " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Note> findByCategoryAfterCursor(@Param("categoryId") Long categoryId,
                                         @Param("createdAt") LocalDateTime createdAt,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    // ── Count ─────────────────────────────────────────────────────────────────
    long countByCategoryId(Long categoryId);

    // ── Search (user-scoped via category owner) ───────────────────────────────
    @Query("SELECT n FROM Note n JOIN FETCH n.category c WHERE c.owner.id = :ownerId AND " +
           "(LOWER(n.title) LIKE LOWER(:pattern) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:pattern)))")
    List<Note> searchByTitleOrContentAndOwnerId(@Param("pattern") String pattern,
                                                 @Param("ownerId") Long ownerId);

    // ── Reorder ───────────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Note n SET n.position = :position WHERE n.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);

    // ── Ownership-safe lookup (loads category + owner in one query) ──────────
    @Query("SELECT n FROM Note n JOIN FETCH n.category c JOIN FETCH c.owner WHERE n.id = :id")
    Optional<Note> findByIdWithCategoryAndOwner(@Param("id") Long id);
}
