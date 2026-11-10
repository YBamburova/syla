package com.syla.core.identity.internal;

import java.util.UUID;
import com.syla.core.ApiFailure;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountRegistration {
    private final AccountRepository accounts;
    AccountRegistration(AccountRepository accounts) { this.accounts = accounts; }

    @Transactional
    public void afterVerifiedLogin(OidcUser user) {
        accounts.register(UUID.randomUUID(), user.getIssuer().toString(), user.getSubject());
        var account = accounts.findByIssuerAndSubject(user.getIssuer().toString(), user.getSubject()).orElseThrow();
        if (account.state.equals("DELETED")) {
            throw new ApiFailure(HttpStatus.FORBIDDEN, "account_deleted", "This account was deleted.");
        }
    }
}
