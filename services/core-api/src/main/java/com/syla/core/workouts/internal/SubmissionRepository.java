package com.syla.core.workouts.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SubmissionRepository extends JpaRepository<Submission, String> {
    void deleteByAthleteId(UUID athleteId);
}
