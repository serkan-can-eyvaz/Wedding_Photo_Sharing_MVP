package com.weddingshare.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    boolean existsByStorageKey(String storageKey);

    List<Media> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);

    long countByEventId(UUID eventId);

    Optional<Media> findByIdAndEventId(UUID id, UUID eventId);

    List<Media> findAllByEventIdAndIdIn(UUID eventId, Collection<UUID> ids);
}
