package com.syla.core.identity.internal;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import com.syla.core.identity.ActorAccess;
import com.syla.core.identity.AccountDeletionStarted;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
class AccountDeletion {
    private final AccountRepository accounts;
    private final ActorAccess actors;
    private final ApplicationEventPublisher events;
    private final KeycloakAccounts provider;
    private final TransactionTemplate transactions;

    AccountDeletion(AccountRepository accounts, ActorAccess actors, ApplicationEventPublisher events,
                    KeycloakAccounts provider, TransactionTemplate transactions) {
        this.accounts = accounts; this.actors = actors; this.events = events;
        this.provider = provider; this.transactions = transactions;
    }

    public boolean deleteCurrent() {
        var id = transactions.execute(status -> {
            var actor = actors.lockForAccountLifecycle();
            var account = accounts.locked(actor.id()).orElseThrow();
            if (account.state.equals("ACTIVE")) {
                account.state = "DELETING";
                events.publishEvent(new AccountDeletionStarted(actor.id()));
            }
            return actor.id();
        });
        return finish(id);
    }

    private boolean finish(UUID id) {
        var account = accounts.findById(id).orElseThrow();
        if (account.state.equals("DELETED")) return true;
        if (!provider.delete(account.issuer, account.subject)) return false;
        transactions.executeWithoutResult(status -> {
            var locked = accounts.locked(id).orElseThrow();
            if (locked.state.equals("DELETING")) {
                locked.state = "DELETED";
                locked.deletedAt = Instant.now();
            }
        });
        return true;
    }

    @Scheduled(fixedDelayString = "${syla.identity.cleanup-delay-ms:60000}")
    void retryAndPurge() {
        for (var account : accounts.findByState("DELETING")) finish(account.id);
        transactions.executeWithoutResult(status -> accounts.purgeTombstones(Instant.now().minus(Duration.ofDays(30))));
    }
}
