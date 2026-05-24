package com.tasknotes.notes.model;

import com.tasknotes.users.model.AppUser;
import jakarta.persistence.*;

@Entity
@Table(name = "note_tag", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
public class NoteTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;  // hex, ex: "#6366f1"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    public Long getId()              { return id; }
    public String getName()          { return name; }
    public void setName(String name) { this.name = name; }
    public String getColor()         { return color; }
    public void setColor(String c)   { this.color = c; }
    public AppUser getOwner()        { return owner; }
    public void setOwner(AppUser o)  { this.owner = o; }
}
