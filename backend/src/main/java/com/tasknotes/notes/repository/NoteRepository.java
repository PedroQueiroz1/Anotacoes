package com.tasknotes.notes.repository;

import com.tasknotes.notes.model.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // ── Legacy ordering (reorder) ─────────────────────────────────────────────
    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId ORDER BY " +
                   "CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, created_at DESC",
           nativeQuery = true)
    List<Note> findByCategoryIdOrdered(@Param("categoryId") Long categoryId);

    // ── Cursor pagination — first page (positioned first, then by created_at) ─
    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId " +
                   "ORDER BY CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, created_at DESC, id DESC",
           nativeQuery = true)
    List<Note> findByCategoryFirstPage(@Param("categoryId") Long categoryId, Pageable pageable);

    // ── Feature 024: filtered + sorted ────────────────────────────────────────
    // pinnedOnly: NULL = all, 1 = only pinned, 0 = only unpinned
    // sort=recent
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, n.created_at DESC, n.id DESC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredRecent(@Param("categoryId") Long categoryId,
                                             @Param("query") String query,
                                             @Param("tagId") Long tagId,
                                             @Param("pinnedOnly") Integer pinnedOnly,
                                             Pageable pageable);

    // sort=oldest
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, n.created_at ASC, n.id ASC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredOldest(@Param("categoryId") Long categoryId,
                                             @Param("query") String query,
                                             @Param("tagId") Long tagId,
                                             @Param("pinnedOnly") Integer pinnedOnly,
                                             Pageable pageable);

    // sort=title_asc
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, n.title ASC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredTitleAsc(@Param("categoryId") Long categoryId,
                                               @Param("query") String query,
                                               @Param("tagId") Long tagId,
                                               @Param("pinnedOnly") Integer pinnedOnly,
                                               Pageable pageable);

    // sort=title_desc
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, n.title DESC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredTitleDesc(@Param("categoryId") Long categoryId,
                                                @Param("query") String query,
                                                @Param("tagId") Long tagId,
                                                @Param("pinnedOnly") Integer pinnedOnly,
                                                Pageable pageable);

    // sort=pinned
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, n.pinned_at DESC, n.created_at DESC, n.id DESC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredPinned(@Param("categoryId") Long categoryId,
                                             @Param("query") String query,
                                             @Param("tagId") Long tagId,
                                             @Param("pinnedOnly") Integer pinnedOnly,
                                             Pageable pageable);

    // sort=manual
    @Query(value = "SELECT DISTINCT n.* FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query))) " +
                   "ORDER BY n.pinned DESC, CASE WHEN n.position IS NULL THEN 1 ELSE 0 END ASC, n.position ASC, n.created_at DESC, n.id DESC",
           nativeQuery = true)
    List<Note> findByCategoryFilteredManual(@Param("categoryId") Long categoryId,
                                             @Param("query") String query,
                                             @Param("tagId") Long tagId,
                                             @Param("pinnedOnly") Integer pinnedOnly,
                                             Pageable pageable);

    // ── Feature 024: count with filters ───────────────────────────────────────
    @Query(value = "SELECT COUNT(DISTINCT n.id) FROM note n " +
                   "LEFT JOIN note_note_tag nnt ON nnt.note_id = n.id " +
                   "WHERE n.category_id = :categoryId " +
                   "AND (:tagId IS NULL OR nnt.tag_id = :tagId) " +
                   "AND (:pinnedOnly IS NULL OR n.pinned = :pinnedOnly) " +
                   "AND (:query IS NULL OR LOWER(n.title) LIKE LOWER(:query) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:query)))",
           nativeQuery = true)
    long countByCategoryFiltered(@Param("categoryId") Long categoryId,
                                  @Param("query") String query,
                                  @Param("tagId") Long tagId,
                                  @Param("pinnedOnly") Integer pinnedOnly);

    // ── Feature 024: position helpers ─────────────────────────────────────────
    @Query(value = "SELECT MIN(n.position) FROM note n WHERE n.category_id = :categoryId AND n.position IS NOT NULL",
           nativeQuery = true)
    Integer findMinPosition(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT MAX(n.position) FROM note n WHERE n.category_id = :categoryId AND n.position IS NOT NULL",
           nativeQuery = true)
    Integer findMaxPosition(@Param("categoryId") Long categoryId);

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

    // ── Load note with tags eagerly ───────────────────────────────────────────
    @Query("SELECT n FROM Note n LEFT JOIN FETCH n.tags WHERE n.id = :id")
    Optional<Note> findByIdWithTags(@Param("id") Long id);
}
