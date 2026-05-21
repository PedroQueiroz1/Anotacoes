package com.tasknotes.concepts.model;

import com.tasknotes.shared.util.UuidV7Generator;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "programming_concept",
    uniqueConstraints = @UniqueConstraint(name = "uk_programming_concept_normalized_term", columnNames = "normalized_term")
)
public class ProgrammingConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true)
    private String uuid;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(name = "normalized_term", nullable = false, length = 100, unique = true)
    private String normalizedTerm;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(length = 50)
    private String source;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount = 0;

    @Column(nullable = false, length = 10)
    private String scope = "GLOBAL";

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    private void prePersist() {
        if (uuid == null) uuid = UuidV7Generator.generate();
    }

    public Long getId()                         { return id; }
    public String getUuid()                     { return uuid; }
    public String getTerm()                     { return term; }
    public void   setTerm(String term)          { this.term = term; }
    public String getNormalizedTerm()           { return normalizedTerm; }
    public void   setNormalizedTerm(String n)   { this.normalizedTerm = n; }
    public String getSummary()                  { return summary; }
    public void   setSummary(String summary)    { this.summary = summary; }
    public String getSource()                   { return source; }
    public void   setSource(String source)      { this.source = source; }
    public String getSourceUrl()                { return sourceUrl; }
    public void   setSourceUrl(String url)      { this.sourceUrl = url; }
    public int    getAcceptedCount()            { return acceptedCount; }
    public void   setAcceptedCount(int c)       { this.acceptedCount = c; }
    public String getScope()                    { return scope; }
    public void   setScope(String scope)        { this.scope = scope; }
    public Long   getOwnerUserId()              { return ownerUserId; }
    public void   setOwnerUserId(Long id)       { this.ownerUserId = id; }
    public LocalDateTime getCreatedAt()         { return createdAt; }
    public LocalDateTime getUpdatedAt()         { return updatedAt; }
    public LocalDateTime getLastUsedAt()        { return lastUsedAt; }
    public void   setLastUsedAt(LocalDateTime t){ this.lastUsedAt = t; }
}
