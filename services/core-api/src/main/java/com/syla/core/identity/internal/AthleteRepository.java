package com.syla.core.identity.internal;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface AthleteRepository extends JpaRepository<Athlete, UUID> {
    Optional<Athlete> findByIdentityKey(String identityKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Athlete a where a.identityKey = :key")
    Optional<Athlete> lockByKey(@Param("key") String key);
    @Modifying
    @Query("delete from Athlete a where a.state = 'DELETED' and a.deletedAt < :before")
    void purgeDeleted(@Param("before") Instant before);
}
