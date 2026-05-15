package com.tasknotes.repository;

import com.tasknotes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query(value = "SELECT * FROM note WHERE category_id = :categoryId ORDER BY " +
                   "CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, created_at DESC",
           nativeQuery = true)
    List<Note> findByCategoryIdOrdered(@Param("categoryId") Long categoryId);

    @Query("SELECT n FROM Note n JOIN FETCH n.category WHERE LOWER(n.title) LIKE LOWER(:pattern) OR (n.content IS NOT NULL AND LOWER(n.content) LIKE LOWER(:pattern))")
    List<Note> searchByTitleOrContent(@Param("pattern") String pattern);

    @Modifying
    @Query("UPDATE Note n SET n.position = :position WHERE n.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);
}
