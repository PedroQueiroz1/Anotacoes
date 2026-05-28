package com.tasknotes.notes.model;

import com.tasknotes.categories.model.Category;
import com.tasknotes.shared.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "note")
public class Note {

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

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column
    private Integer position;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "note_note_tag",
        joinColumns = @JoinColumn(name = "note_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<NoteTag> tags = new ArrayList<>();

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

    public Long getId()                       { return id; }
    public String getUuid()                   { return uuid; }
    public void setUuid(String uuid)          { this.uuid = uuid; }
    public Category getCategory()             { return category; }
    public void setCategory(Category c)       { this.category = c; }
    public String getTitle()                  { return title; }
    public void setTitle(String t)            { this.title = t; }
    public String getContent()                { return content; }
    public void setContent(String c)          { this.content = c; }
    public String getContentHtml()            { return contentHtml; }
    public void setContentHtml(String h)      { this.contentHtml = h; }
    public Integer getPosition()              { return position; }
    public void setPosition(Integer p)        { this.position = p; }
    public boolean isPinned()                 { return pinned; }
    public void setPinned(boolean pinned)     { this.pinned = pinned; }
    public LocalDateTime getPinnedAt()        { return pinnedAt; }
    public void setPinnedAt(LocalDateTime t)  { this.pinnedAt = t; }
    public List<NoteTag> getTags()            { return tags; }
    public void setTags(List<NoteTag> tags)   { this.tags = tags; }
    public LocalDateTime getCreatedAt()       { return createdAt; }
    public LocalDateTime getUpdatedAt()       { return updatedAt; }
}
