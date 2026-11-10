package com.syla.core.identity.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountEntity> findByIssuerAndSubject(String issuer, String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountEntity account where account.id = :id")
    Optional<AccountEntity> locked(UUID id);

    @Modifying
    @Query(value = "insert into identity.accounts(id,issuer,subject,state,created_at) "
            + "values (:id,:issuer,:subject,'ACTIVE',CURRENT_TIMESTAMP) on conflict(issuer,subject) do nothing", nativeQuery = true)
    void register(UUID id, String issuer, String subject);

    List<AccountEntity> findByState(String state);

    @Modifying
    @Query("delete from AccountEntity account where account.state = 'DELETED' and account.deletedAt < :before")
    void purgeTombstones(Instant before);
}
