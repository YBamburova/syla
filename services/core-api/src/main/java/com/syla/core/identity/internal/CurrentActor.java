package com.syla.core.identity.internal;

import com.syla.core.ApiFailure;
import com.syla.core.identity.ActorAccess;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CurrentActor implements ActorAccess {
    private final AccountRepository accounts;
    CurrentActor(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Actor lockActive() { return query(false); }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Actor lockForAccountLifecycle() { return query(true); }

    private Actor query(boolean allowDeleting) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser user)) {
            throw new ApiFailure(HttpStatus.UNAUTHORIZED, "authentication_required", "Sign in first.");
        }
        var account = accounts.findByIssuerAndSubject(user.getIssuer().toString(), user.getSubject())
                .orElseThrow(() -> new ApiFailure(HttpStatus.UNAUTHORIZED, "account_unavailable", "Sign in again."));
        if (!account.state.equals("ACTIVE") && !(allowDeleting && account.state.equals("DELETING"))) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "account_inactive", "Account access is blocked.");
        }
        return new Actor(account.id, account.issuer, account.subject, account.state, account.createdAt);
    }
}
