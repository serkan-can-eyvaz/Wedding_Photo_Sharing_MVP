package com.weddingshare.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    boolean existsByStorageKey(String storageKey);

    List<Media> findAllByEventIdOrderByCreatedAtDesc(UUID eventId);

    List<Media> findByEventIdOrderByCreatedAtDescIdDesc(UUID eventId, Pageable pageable);

    @Query("""
            select media from Media media
            where media.event.id = :eventId
              and (media.createdAt < :createdAt
                   or (media.createdAt = :createdAt and media.id < :mediaId))
            order by media.createdAt desc, media.id desc
            """)
    List<Media> findNextPageByEventId(
            @Param("eventId") UUID eventId,
            @Param("createdAt") java.time.Instant createdAt,
            @Param("mediaId") UUID mediaId,
            Pageable pageable
    );

    long countByEventId(UUID eventId);

    Optional<Media> findByIdAndEventId(UUID id, UUID eventId);

    List<Media> findAllByEventIdAndIdIn(UUID eventId, Collection<UUID> ids);
}
