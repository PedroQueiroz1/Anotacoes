package com.tasknotes.repository;

import com.tasknotes.model.Task;
import com.tasknotes.model.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // ── Legacy ordering (sidebar summary / reorder) ───────────────────────────
    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId ORDER BY " +
                   "CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, created_at ASC",
           nativeQuery = true)
    List<Task> findByCategoryIdOrdered(@Param("categoryId") Long categoryId);

    // ── Cursor pagination — first page ────────────────────────────────────────
    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Task> findByCategoryFirstPage(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId AND status = :status " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Task> findByCategoryFirstPageByStatus(@Param("categoryId") Long categoryId,
                                               @Param("status") String status,
                                               Pageable pageable);

    // ── Cursor pagination — after cursor ──────────────────────────────────────
    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId " +
                   "AND (created_at < :createdAt OR (created_at = :createdAt AND id < :cursorId)) " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Task> findByCategoryAfterCursor(@Param("categoryId") Long categoryId,
                                         @Param("createdAt") LocalDateTime createdAt,
                                         @Param("cursorId") Long cursorId,
                                         Pageable pageable);

    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId AND status = :status " +
                   "AND (created_at < :createdAt OR (created_at = :createdAt AND id < :cursorId)) " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Task> findByCategoryAfterCursorByStatus(@Param("categoryId") Long categoryId,
                                                 @Param("status") String status,
                                                 @Param("createdAt") LocalDateTime createdAt,
                                                 @Param("cursorId") Long cursorId,
                                                 Pageable pageable);

    // ── Counts ────────────────────────────────────────────────────────────────
    long countByCategoryIdAndStatusNot(Long categoryId, TaskStatus status);

    // ── Search ────────────────────────────────────────────────────────────────
    @Query("SELECT t FROM Task t JOIN FETCH t.category WHERE LOWER(t.title) LIKE LOWER(:pattern) OR (t.description IS NOT NULL AND LOWER(t.description) LIKE LOWER(:pattern))")
    List<Task> searchByTitleOrDescription(@Param("pattern") String pattern);

    // ── Reorder ───────────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE Task t SET t.position = :position WHERE t.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);
}
