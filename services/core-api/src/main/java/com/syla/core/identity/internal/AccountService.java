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

    @Override @Transactional
    public AccountView current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser)) {
            return new AccountView(false, null, null, null, false, CONSENT_VERSION, null);
        }
        OidcUser user = user(authentication);
        Athlete athlete = account(authentication);
        String name = user.getPreferredUsername();
        return new AccountView(true, athlete.id, name == null ? "Athlete" : name, athlete.state,
                CONSENT_VERSION.equals(athlete.consentVersion), CONSENT_VERSION, athlete.consentedAt);
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

    @Override @Transactional
    public void consent(Authentication authentication) {
        Athlete athlete = account(authentication);
        if (!athlete.state.equals("ACTIVE")) throw new ApiProblem(403, "This account is being deleted.");
        if (!CONSENT_VERSION.equals(athlete.consentVersion)) {
            athlete.consentVersion = CONSENT_VERSION;
            athlete.consentedAt = Instant.now();
        }
    }

    @Override @Transactional
    public UUID beginDeletion(Authentication authentication) {
        Athlete athlete = account(authentication);
        if (athlete.state.equals("DELETED")) return athlete.id;
        athlete.state = "DELETING";
        athlete.consentVersion = null;
        athlete.consentedAt = null;
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
