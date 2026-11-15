package com.syla.core.identity.internal;

import com.syla.core.identity.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/profile") @Tag(name = "Profiles")
class ProfileController {
    private final ProfileService profiles;
    ProfileController(ProfileService profiles) { this.profiles = profiles; }
    record ProfileState(ProfileView profile) {}
    @GetMapping @Operation(operationId = "getOwnProfile", summary = "Your opt-in athlete profile, or an empty response before setup")
    ProfileState own(Authentication auth) { return new ProfileState(profiles.own(auth)); }
    @PutMapping @Operation(operationId = "saveProfile", summary = "Opt in to discovery by signed-in athletes and edit your profile")
    ProfileView save(Authentication auth, @Valid @RequestBody ProfileInput input) { return profiles.save(auth, input); }
}
