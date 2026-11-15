package com.syla.core.workouts.internal;
import com.syla.core.workouts.SharedActivityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @Tag(name="Shared activities")
class SharedActivityController {
    private final WorkoutService workouts;
    SharedActivityController(WorkoutService workouts) { this.workouts=workouts; }
    @GetMapping("/athletes/{owner}/activities") @Operation(operationId="listSharedActivities", summary="Permitted follower summaries, newest first")
    SharedActivityView.SharedPage list(Authentication auth,@PathVariable UUID owner,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return workouts.sharedList(auth,owner,page,size); }
    @GetMapping("/shared-activities/{id}") @Operation(operationId="getSharedActivity", summary="A permitted activity summary; private details are excluded")
    SharedActivityView get(Authentication auth,@PathVariable UUID id) { return workouts.sharedGet(auth,id); }
}
