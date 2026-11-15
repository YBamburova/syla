package com.syla.core.identity;

import java.util.*;
import org.springframework.security.core.Authentication;

public interface AthleteDirectory {
    UUID actor(Authentication auth);
    /** Lock participants in canonical order before a relationship mutation. */
    void lockParticipants(UUID first, UUID second);
    ProfileView profile(UUID id);
    ProfileView byHandle(String handle);
    List<ProfileView> search(String query, UUID viewer, Set<UUID> excluded);
    ProfileView own(Authentication auth);
    void deleteProfile(UUID id);
}
