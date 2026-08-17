package com.weddingshare.event;

import com.weddingshare.media.Media;
import com.weddingshare.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    private static final int VIEWER_TOKEN_BYTES = 32;
    private static final SecureRandom VIEWER_TOKEN_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "public_token", nullable = false, unique = true)
    private String publicToken;

    @Column(name = "viewer_token", nullable = false, unique = true)
    private String viewerToken;

    @Column(name = "cover_image_key")
    private String coverImageKey;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "event")
    private List<Media> media = new ArrayList<>();

    protected Event() {
    }

    public Event(User owner, String name, LocalDate eventDate, String publicToken, String viewerToken, String coverImageKey, boolean active) {
        this.owner = owner;
        this.name = name;
        this.eventDate = eventDate;
        this.publicToken = publicToken;
        this.viewerToken = viewerToken;
        this.coverImageKey = coverImageKey;
        this.active = active;
    }

    public Event(User owner, String name, LocalDate eventDate, String publicToken, String coverImageKey, boolean active) {
        this(owner, name, eventDate, publicToken, generateViewerToken(), coverImageKey, active);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public String getViewerToken() {
        return viewerToken;
    }

    void setViewerToken(String viewerToken) {
        this.viewerToken = viewerToken;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, LocalDate eventDate, String coverImageKey, boolean active) {
        this.name = name;
        this.eventDate = eventDate;
        this.coverImageKey = coverImageKey;
        this.active = active;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static String generateViewerToken() {
        byte[] bytes = new byte[VIEWER_TOKEN_BYTES];
        VIEWER_TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
