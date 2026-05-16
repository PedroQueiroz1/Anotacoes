package com.tasknotes.model;

import com.tasknotes.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 36, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskStatus status = TaskStatus.TODO;

    @Column
    private Integer position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        if (this.uuid == null) this.uuid = UuidV7Generator.generate();
    }

    public Long getId()                  { return id; }
    public String getUuid()              { return uuid; }
    public void setUuid(String uuid)     { this.uuid = uuid; }
    public Category getCategory()        { return category; }
    public void setCategory(Category c)  { this.category = c; }
    public String getTitle()             { return title; }
    public void setTitle(String t)       { this.title = t; }
    public String getDescription()       { return description; }
    public void setDescription(String d) { this.description = d; }
    public LocalDate getDueDate()        { return dueDate; }
    public void setDueDate(LocalDate d)  { this.dueDate = d; }
    public Priority getPriority()        { return priority; }
    public void setPriority(Priority p)  { this.priority = p; }
    public TaskStatus getStatus()        { return status; }
    public void setStatus(TaskStatus s)  { this.status = s; }
    public Integer getPosition()          { return position; }
    public void setPosition(Integer p)   { this.position = p; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
}
