package com.airguard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int xp = 0;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private int level = 1;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int streakDays = 0;

    private LocalDateTime lastActive;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Activity> activities;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {}

    private User(Builder b) {
        this.id         = b.id;
        this.username   = b.username;
        this.email      = b.email;
        this.password   = b.password;
        this.xp         = b.xp;
        this.level      = b.level;
        this.streakDays = b.streakDays;
        this.lastActive = b.lastActive;
        this.createdAt  = b.createdAt != null ? b.createdAt : LocalDateTime.now();
        this.updatedAt  = b.updatedAt != null ? b.updatedAt : LocalDateTime.now();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String username, email, password;
        private int xp = 0, level = 1, streakDays = 0;
        private LocalDateTime lastActive, createdAt, updatedAt;

        public Builder id(Long id)                       { this.id = id; return this; }
        public Builder username(String username)         { this.username = username; return this; }
        public Builder email(String email)               { this.email = email; return this; }
        public Builder password(String password)         { this.password = password; return this; }
        public Builder xp(int xp)                        { this.xp = xp; return this; }
        public Builder level(int level)                  { this.level = level; return this; }
        public Builder streakDays(int streakDays)        { this.streakDays = streakDays; return this; }
        public Builder lastActive(LocalDateTime v)       { this.lastActive = v; return this; }
        public Builder createdAt(LocalDateTime v)        { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v)        { this.updatedAt = v; return this; }
        public User build()                              { return new User(this); }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public String getEmail()             { return email; }
    public String getPassword()          { return password; }
    public int getXp()                   { return xp; }
    public int getLevel()                { return level; }
    public int getStreakDays()           { return streakDays; }
    public LocalDateTime getLastActive() { return lastActive; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getUpdatedAt()  { return updatedAt; }
    public List<Activity> getActivities(){ return activities; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(Long id)                        { this.id = id; }
    public void setUsername(String username)          { this.username = username; }
    public void setEmail(String email)                { this.email = email; }
    public void setPassword(String password)          { this.password = password; }
    public void setXp(int xp)                         { this.xp = xp; }
    public void setLevel(int level)                   { this.level = level; }
    public void setStreakDays(int streakDays)         { this.streakDays = streakDays; }
    public void setLastActive(LocalDateTime v)        { this.lastActive = v; }
    public void setCreatedAt(LocalDateTime v)         { this.createdAt = v; }
    public void setUpdatedAt(LocalDateTime v)         { this.updatedAt = v; }
    public void setActivities(List<Activity> acts)    { this.activities = acts; }
}