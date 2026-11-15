package com.syla.core.identity.internal;

import com.syla.core.identity.*;
import com.syla.core.platform.ApiProblem;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ProfileService implements AthleteDirectory {
    private final AthleteAccess access;
    private final JdbcTemplate jdbc;
    ProfileService(AthleteAccess access, JdbcTemplate jdbc) { this.access = access; this.jdbc = jdbc; }

    public UUID actor(Authentication auth) { return access.activeId(auth); }

    public void lockParticipants(UUID first, UUID second) {
        // Shared by all pair mutations, including reciprocal requests and blocks.
        for (UUID id : java.util.stream.Stream.of(first, second).distinct().sorted().toList()) {
            var states = jdbc.queryForList("select state from identity.athlete where id = ? for update", String.class, id);
            if (states.isEmpty() || !states.getFirst().equals("ACTIVE")) throw missing();
        }
    }

    private ProfileView row(java.sql.ResultSet rs, int index) throws java.sql.SQLException {
        return new ProfileView(rs.getObject("athlete_id", UUID.class), rs.getString("handle"),
                rs.getString("display_name"), rs.getString("bio"), rs.getBoolean("private_profile"));
    }
    private List<ProfileView> profiles(String condition, Object... args) {
        return jdbc.query("select p.* from identity.profile p join identity.athlete a on a.id=p.athlete_id where a.state='ACTIVE' and " + condition, this::row, args);
    }
    public ProfileView profile(UUID id) {
        return profiles("p.athlete_id = ?", id).stream().findFirst().orElseThrow(ProfileService::missing);
    }
    public ProfileView byHandle(String handle) {
        return profiles("p.handle = ?", handle.toLowerCase(Locale.ROOT)).stream().findFirst().orElseThrow(ProfileService::missing);
    }
    public ProfileView own(Authentication auth) {
        return profiles("p.athlete_id = ?", actor(auth)).stream().findFirst().orElse(null);
    }
    public List<ProfileView> search(String query, UUID viewer, Set<UUID> excluded) {
        String term = query.strip().toLowerCase(Locale.ROOT);
        if (term.length() < 2 || term.length() > 80) throw new ApiProblem(400, "Search with 2–80 characters.");
        // literal substring search avoids wildcard injection; exclusions apply before the bounded result.
        List<Object> args = new ArrayList<>(List.of(viewer, term, term, term, term, term, term));
        String filter = "";
        if (!excluded.isEmpty()) {
            filter = " and p.athlete_id not in (" + String.join(",", Collections.nCopies(excluded.size(), "?")) + ")";
            args.addAll(excluded);
        }
        return profiles("p.athlete_id <> ? and (strpos(p.handle, ?) > 0 or strpos(lower(p.display_name), ?) > 0 or strpos(lower(coalesce(a.username,'')), ?) > 0 or strpos(lower(coalesce(a.first_name,'')), ?) > 0 or strpos(lower(coalesce(a.last_name,'')), ?) > 0 or strpos(lower(concat_ws(' ',a.first_name,a.last_name)), ?) > 0)" + filter + " order by p.handle limit 20", args.toArray());
    }
    ProfileView save(Authentication auth, ProfileInput input) {
        UUID id = access.lockActive(auth, true);
        try {
            jdbc.update("insert into identity.profile(athlete_id, handle, display_name, bio, private_profile) values (?,?,?,?,?) on conflict(athlete_id) do update set handle=excluded.handle, display_name=excluded.display_name, bio=excluded.bio, private_profile=excluded.private_profile", id, input.handle(), input.displayName().strip(), input.bio().strip(), input.privateProfile());
        } catch (org.springframework.dao.DuplicateKeyException conflict) {
            throw new ApiProblem(409, "This handle is already taken.", Map.of("handle", "Choose another handle."));
        }
        return profile(id);
    }
    public void deleteProfile(UUID id) {
        jdbc.update("delete from identity.profile where athlete_id=?", id);
    }
    private static ApiProblem missing() { return new ApiProblem(404, "Athlete not found."); }
}
