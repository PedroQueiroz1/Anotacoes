package com.tasknotes.repository;

import com.tasknotes.model.Task;
import com.tasknotes.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByCategoryId(Long categoryId);

    long countByCategoryIdAndStatusNot(Long categoryId, TaskStatus status);
}
