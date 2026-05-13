package com.tasknotes.repository;

import com.tasknotes.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
}
