package com.tasknotes.tasks.repository;

import com.tasknotes.tasks.model.Subtask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    // ── Legacy (kept for backfill / export) ───────────────────────────────────
    List<Subtask> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    // ── Cursor pagination — first page ────────────────────────────────────────
    @Query(value = "SELECT * FROM subtask WHERE task_id = :taskId " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Subtask> findByTaskFirstPage(@Param("taskId") Long taskId, Pageable pageable);

    // ── Cursor pagination — after cursor ──────────────────────────────────────
    @Query(value = "SELECT * FROM subtask WHERE task_id = :taskId " +
                   "AND (created_at < :createdAt OR (created_at = :createdAt AND id < :cursorId)) " +
                   "ORDER BY created_at DESC, id DESC",
           nativeQuery = true)
    List<Subtask> findByTaskAfterCursor(@Param("taskId") Long taskId,
                                        @Param("createdAt") LocalDateTime createdAt,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    // ── Counts ────────────────────────────────────────────────────────────────
    long countByTaskId(Long taskId);

    long countByTaskIdAndDone(Long taskId, boolean done);

    // ── Search (user-scoped via task → category → owner) ─────────────────────
    @Query("SELECT s FROM Subtask s JOIN FETCH s.task t JOIN FETCH t.category c " +
           "WHERE c.owner.id = :ownerId AND LOWER(s.text) LIKE LOWER(:pattern)")
    List<Subtask> searchByTextAndOwnerId(@Param("pattern") String pattern,
                                          @Param("ownerId") Long ownerId);

}
