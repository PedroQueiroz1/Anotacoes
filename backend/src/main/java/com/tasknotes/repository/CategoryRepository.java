package com.tasknotes.repository;

import com.tasknotes.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c ORDER BY " +
           "CASE WHEN c.position IS NULL THEN 1 ELSE 0 END ASC, " +
           "c.position ASC, c.createdAt ASC")
    List<Category> findAllOrdered();

    List<Category> findByNameContainingIgnoreCase(String name);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Modifying
    @Query("UPDATE Category c SET c.position = :position WHERE c.id = :id")
    void updatePosition(@Param("id") Long id, @Param("position") int position);
}
