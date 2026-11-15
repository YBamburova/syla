package com.syla.core.social.internal;

import com.syla.core.identity.ProfileView;
import com.syla.core.social.AthleteView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @Tag(name="Social")
class SocialController {
    private final SocialService social;
    SocialController(SocialService social) { this.social=social; }
    @GetMapping("/athletes") @Operation(operationId="searchAthletes", summary="Search opt-in profiles; at most 20 results")
    List<ProfileView> search(Authentication auth,@RequestParam String query) { return social.search(auth,query); }
    @GetMapping("/athletes/by-handle/{handle}") @Operation(operationId="getAthlete", summary="View an athlete profile and your relationship")
    AthleteView get(Authentication auth,@PathVariable String handle) { return social.get(auth,handle); }
    @GetMapping("/social/{kind}") @Operation(operationId="listRelationships", summary="Your following, followers, requests, pending or blocks; 20 per page")
    List<ProfileView> list(Authentication auth,@PathVariable String kind,@RequestParam(defaultValue="0") int page) { return social.list(auth,kind,page); }
    @PostMapping("/social/{target}/{action}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId="changeRelationship", summary="Follow, unfollow, accept, remove, block or unblock an athlete")
    void change(Authentication auth,@PathVariable UUID target,@PathVariable String action) { social.change(auth,target,action); }
}
