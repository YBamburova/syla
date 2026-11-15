package com.syla.core.identity.internal;

import com.syla.core.identity.AccountView;
import com.syla.core.identity.AthleteAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
@Tag(name = "Account")
class SessionController {
    private final AthleteAccess access;
    SessionController(AthleteAccess access) { this.access = access; }

    @GetMapping
    @Operation(operationId = "getSession", summary = "Read sign-in and training consent state")
    ResponseEntity<AccountView> current(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(access.current(authentication));
    }

    record CsrfView(String headerName, String parameterName, String token) {}

    @GetMapping("/csrf")
    @Operation(operationId = "getCsrf", summary = "Get a session-bound CSRF token before mutations")
    ResponseEntity<CsrfView> csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfView(token.getHeaderName(), token.getParameterName(), token.getToken()));
    }

    @PostMapping("/consent")
    @Operation(operationId = "acceptConsent", summary = "Consent to storing training details for logging and personal history")
    AccountView consent(Authentication authentication) {
        access.consent(authentication);
        return access.current(authentication);
    }
}
