package com.tasknotes.model;

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

    public Long getId()               { return id; }
    public Task getTask()             { return task; }
    public void setTask(Task t)       { this.task = t; }
    public String getText()           { return text; }
    public void setText(String t)     { this.text = t; }
    public boolean isDone()           { return done; }
    public void setDone(boolean done) { this.done = done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
