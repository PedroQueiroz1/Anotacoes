package com.tasknotes.repository;

import com.tasknotes.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    long countByTaskId(Long taskId);

    @Query("SELECT s FROM Subtask s JOIN FETCH s.task t JOIN FETCH t.category WHERE LOWER(s.text) LIKE LOWER(:pattern)")
    List<Subtask> searchByText(@Param("pattern") String pattern);
}
