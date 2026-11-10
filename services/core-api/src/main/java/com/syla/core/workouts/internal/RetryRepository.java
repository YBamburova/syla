package com.syla.core.workouts.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

interface RetryRepository extends JpaRepository<RetryEntity, UUID> {
    Optional<RetryEntity> findByActorIdAndKey(UUID actorId, UUID key);
    List<RetryEntity> findByActorId(UUID actorId);
    void deleteByActorId(UUID actorId);
    @Modifying
    @Query("delete from RetryEntity request where request.expiresAt < :before")
    void purgeExpired(Instant before);
}
