package com.tasknotes.dictionary.model;

import com.tasknotes.shared.util.UuidV7Generator;
import com.tasknotes.users.model.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "dictionary_entry",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_dictionary_entry_user_normalized_term",
        columnNames = {"user_id", "normalized_term"}
    )
)
public class DictionaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", length = 36, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(name = "normalized_term", nullable = false, length = 100)
    private String normalizedTerm;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String definition;

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

    public Long getId()                          { return id; }
    public String getUuid()                      { return uuid; }
    public void setUuid(String uuid)             { this.uuid = uuid; }
    public AppUser getUser()                     { return user; }
    public void setUser(AppUser user)            { this.user = user; }
    public String getTerm()                      { return term; }
    public void setTerm(String term)             { this.term = term; }
    public String getNormalizedTerm()            { return normalizedTerm; }
    public void setNormalizedTerm(String n)      { this.normalizedTerm = n; }
    public String getDefinition()                { return definition; }
    public void setDefinition(String definition) { this.definition = definition; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }
}
