package com.tasknotes.repository;

import com.tasknotes.model.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    long countByTaskId(Long taskId);
}
