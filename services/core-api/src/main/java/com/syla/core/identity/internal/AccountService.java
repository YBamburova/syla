package com.syla.core.identity.internal;

import com.syla.core.identity.AccountView;
import com.syla.core.identity.AthleteAccess;
import com.syla.core.platform.ApiProblem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountService implements AthleteAccess {
    static final String CONSENT_VERSION = "training-v1";
    private final AthleteRepository athletes;
    private final JdbcTemplate jdbc;

    AccountService(AthleteRepository athletes, JdbcTemplate jdbc) { this.athletes = athletes; this.jdbc = jdbc; }

    private OidcUser user(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser user)) {
            throw new ApiProblem(401, "Sign in to continue.");
        }
        return user;
    }

    private String key(OidcUser user) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((user.getIssuer() + "\n" + user.getSubject()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private Athlete account(Authentication authentication) {
        String key = key(user(authentication));
        // Unique identity mapping remains safe when two initial browser requests race.
        jdbc.update("insert into identity.athlete(id, identity_key, state) values (?, ?, 'ACTIVE') on conflict(identity_key) do nothing", UUID.randomUUID(), key);
        return athletes.lockByKey(key).orElseThrow();
    }

    @Override @Transactional(readOnly = true)
    public UUID activeId(Authentication authentication) {
        var athlete = athletes.findByIdentityKey(key(user(authentication)))
                .orElseThrow(() -> new ApiProblem(403, "Confirm consent first."));
        if (!athlete.state.equals("ACTIVE") || !CONSENT_VERSION.equals(athlete.consentVersion))
            throw new ApiProblem(403, "An active account with training consent is required.");
        return athlete.id;
    }

    @Override @Transactional
    public AccountView current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser)) {
            return new AccountView(false, null, null, null, false, CONSENT_VERSION, null, null, null, null);
        }
        OidcUser user = user(authentication);
        Athlete athlete = account(authentication);
        if (athlete.state.equals("ACTIVE") && CONSENT_VERSION.equals(athlete.consentVersion))
            syncRegistration(athlete, user);
        String name = user.getPreferredUsername();
        return new AccountView(true, athlete.id, name == null ? "Athlete" : name, athlete.state,
                CONSENT_VERSION.equals(athlete.consentVersion), CONSENT_VERSION, athlete.consentedAt, athlete.username, athlete.firstName, athlete.lastName);
    }

    @Override @Transactional
    public UUID lockActive(Authentication authentication, boolean consentRequired) {
        Athlete athlete = account(authentication);
        if (!athlete.state.equals("ACTIVE")) throw new ApiProblem(403, "Account deletion is in progress. Training access is blocked.");
        if (consentRequired && !CONSENT_VERSION.equals(athlete.consentVersion)) {
            throw new ApiProblem(403, "Confirm training-data consent before logging activities.");
        }
        return athlete.id;
    }

    private String bounded(String value, int length) {
        if (value == null) return "";
        String text = value.strip();
        int end = Math.min(text.length(), length);
        if (end > 0 && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end);
    }

    private void syncRegistration(Athlete athlete, OidcUser user) {
        athlete.username = bounded(user.getPreferredUsername(), 255);
        athlete.firstName = bounded(user.getGivenName(), 255);
        athlete.lastName = bounded(user.getFamilyName(), 255);
        String name = bounded((athlete.firstName + " " + athlete.lastName).strip(), 80);
        if (name.isEmpty()) name = bounded(athlete.username, 80);
        if (name.isEmpty()) name = "Athlete";
        if (Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from identity.profile where athlete_id=?)", Boolean.class, athlete.id))) return;
        String handle = athlete.username.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (handle.isEmpty() || !Character.isLetter(handle.charAt(0))) handle = "athlete_" + handle;
        if (handle.length() < 3) handle += "_athlete";
        handle = bounded(handle, 30);
        for (int attempt = 0; attempt < 5; attempt++) {
            int inserted = jdbc.update("insert into identity.profile(athlete_id, handle, display_name, bio, private_profile) values (?,?,?,'',true) on conflict do nothing", athlete.id, handle, name);
            if (inserted == 1) return;
            handle = "athlete_" + UUID.randomUUID().toString().replace("-", "").substring(0,22);
        }
        throw new ApiProblem(409, "Could not assign an athlete handle. Please retry.");
    }

    @Override @Transactional
    public void consent(Authentication authentication) {
        Athlete athlete = account(authentication);
        if (!athlete.state.equals("ACTIVE")) throw new ApiProblem(403, "This account is being deleted.");
        if (!CONSENT_VERSION.equals(athlete.consentVersion)) {
            athlete.consentVersion = CONSENT_VERSION;
            athlete.consentedAt = Instant.now();
        }
        syncRegistration(athlete, user(authentication));
    }

    @Override @Transactional
    public UUID beginDeletion(Authentication authentication) {
        Athlete athlete = account(authentication);
        if (athlete.state.equals("DELETED")) return athlete.id;
        athlete.state = "DELETING";
        athlete.consentVersion = null;
        athlete.consentedAt = null;
        athlete.username = null;
        athlete.firstName = null;
        athlete.lastName = null;
        return athlete.id;
    }

    @Override @Transactional
    public void completeDeletion(Authentication authentication) {
        Athlete athlete = account(authentication);
        athlete.state = "DELETED";
        athlete.deletedAt = Instant.now();
    }

    @Scheduled(fixedDelay = 3600000) @Transactional
    void purgeExpiredBarriers() {
        // The provider account is deleted before this state is set; outlive every 12-hour app session.
        athletes.purgeDeleted(Instant.now().minus(24, ChronoUnit.HOURS));
    }
}
