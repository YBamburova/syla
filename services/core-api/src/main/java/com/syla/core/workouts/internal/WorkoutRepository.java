package com.syla.core.workouts.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

interface WorkoutRepository extends JpaRepository<WorkoutEntity, UUID> {
    Optional<WorkoutEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<WorkoutEntity> findByOwnerIdOrderByTrainingDateDescIdDesc(UUID ownerId);
    void deleteByOwnerId(UUID ownerId);

    @Query(value = "select * from workouts.sessions where owner_id = :ownerId "
            + "and (cast(:date as date) is null or training_date < :date "
            + "or (training_date = :date and id < :id)) order by training_date desc,id desc limit :limit", nativeQuery = true)
    List<WorkoutEntity> page(UUID ownerId, LocalDate date, UUID id, int limit);
}
