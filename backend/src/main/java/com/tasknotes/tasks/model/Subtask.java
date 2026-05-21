package com.tasknotes.tasks.model;

import com.tasknotes.shared.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "subtask")
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 36, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false)
    private boolean done = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (this.uuid == null) this.uuid = UuidV7Generator.generate();
    }

    public Long getId()               { return id; }
    public String getUuid()           { return uuid; }
    public void setUuid(String uuid)  { this.uuid = uuid; }
    public Task getTask()             { return task; }
    public void setTask(Task t)       { this.task = t; }
    public String getText()           { return text; }
    public void setText(String t)     { this.text = t; }
    public boolean isDone()           { return done; }
    public void setDone(boolean done) { this.done = done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
