package com.syla.core.privacy.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConsentRepository extends JpaRepository<ConsentEntity, UUID> {
    Optional<ConsentEntity> findFirstByActorIdOrderByRecordedAtDescIdDesc(UUID actorId);
    List<ConsentEntity> findByActorIdOrderByRecordedAtAscIdAsc(UUID actorId);
    void deleteByActorId(UUID actorId);
}
