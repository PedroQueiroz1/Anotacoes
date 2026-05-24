package com.tasknotes.notes.repository;

import com.tasknotes.notes.model.NoteTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteTagRepository extends JpaRepository<NoteTag, Long> {
    List<NoteTag> findByOwnerIdOrderByNameAsc(Long ownerId);
    Optional<NoteTag> findByOwnerIdAndNameIgnoreCase(Long ownerId, String name);
}
