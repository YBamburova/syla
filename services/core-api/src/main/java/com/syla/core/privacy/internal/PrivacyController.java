package com.syla.core.privacy.internal;

import com.syla.core.identity.*;
import com.syla.core.workouts.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Privacy")
class PrivacyController {
    private final AthleteAccess access;
    private final WorkoutData workouts;
    private final IdentityLifecycle lifecycle;

    PrivacyController(AthleteAccess access, WorkoutData workouts, IdentityLifecycle lifecycle) {
        this.access = access; this.workouts = workouts; this.lifecycle = lifecycle;
    }

    record Export(String schemaVersion, Instant exportedAt, AccountView account, List<WorkoutView> activities) {}

    @GetMapping("/export")
    @Operation(operationId = "exportAccount", summary = "Export your stored profile/consent and private activities as JSON")
    ResponseEntity<Export> export(Authentication auth) {
        var result = new Export("1", Instant.now(), access.current(auth), workouts.export(auth));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=syla-export.json").body(result);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteAccount", summary = "Withdraw training consent, block mutations, delete training data and provider identity; retry on provider failures")
    void delete(Authentication auth, HttpServletRequest request) {
        var owner = access.beginDeletion(auth);
        workouts.deleteAllForAccount(owner);
        lifecycle.deleteProviderAccount(auth);
        access.completeDeletion(auth);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
    }
}
