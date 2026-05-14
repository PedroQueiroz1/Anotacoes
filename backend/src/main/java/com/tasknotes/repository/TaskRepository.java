package com.tasknotes.repository;

import com.tasknotes.model.Task;
import com.tasknotes.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(value = "SELECT * FROM task WHERE category_id = :categoryId ORDER BY " +
                   "CASE WHEN position IS NULL THEN 1 ELSE 0 END ASC, " +
                   "position ASC, " +
                   "CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC, " +
                   "created_at ASC",
           nativeQuery = true)
    List<Task> findByCategoryIdOrdered(@Param("categoryId") Long categoryId);

    long countByCategoryIdAndStatusNot(Long categoryId, TaskStatus status);

    @Modifying
    @Query("UPDATE Task t SET t.position = :position WHERE t.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);
}
