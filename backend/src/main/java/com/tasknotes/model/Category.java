package com.tasknotes.model;

import com.tasknotes.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "category",
    uniqueConstraints = @UniqueConstraint(name = "uk_category_slug_owner", columnNames = {"slug", "owner_id"})
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 36, unique = true)
    private String uuid;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 70)
    private String slug;

    @Column
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

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

    public Long getId()                        { return id; }
    public String getUuid()                    { return uuid; }
    public void setUuid(String uuid)           { this.uuid = uuid; }
    public String getName()                    { return name; }
    public void setName(String name)           { this.name = name; }
    public String getSlug()                    { return slug; }
    public void setSlug(String slug)           { this.slug = slug; }
    public Integer getPosition()               { return position; }
    public void setPosition(Integer position)  { this.position = position; }
    public AppUser getOwner()                  { return owner; }
    public void setOwner(AppUser owner)        { this.owner = owner; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }
}
