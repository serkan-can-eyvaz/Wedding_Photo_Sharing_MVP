package com.weddingshare.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    boolean existsByStorageKey(String storageKey);

    List<Media> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);
}
