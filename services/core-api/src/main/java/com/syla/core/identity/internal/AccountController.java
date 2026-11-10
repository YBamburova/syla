package com.syla.core.identity.internal;

import com.syla.core.identity.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.support.TransactionTemplate;

@Controller
class AccountController implements AccountApi {
    private final ActorAccess actors;
    private final AccountDeletion deletion;
    private final TransactionTemplate transactions;
    private final HttpServletRequest request;

    AccountController(ActorAccess actors, AccountDeletion deletion, TransactionTemplate transactions, HttpServletRequest request) {
        this.actors = actors; this.deletion = deletion; this.transactions = transactions; this.request = request;
    }

    @Override
    public ResponseEntity<AccountView> getAccount() {
        return ResponseEntity.ok(transactions.execute(status -> {
            var actor = actors.lockForAccountLifecycle();
            return new AccountView(actor.id(), AccountView.StateEnum.fromValue(actor.state()));
        }));
    }

    @Override
    public ResponseEntity<CsrfResponse> csrf() {
        var token = (CsrfToken) request.getAttribute(BrowserSecurity.CSRF_RAW);
        return ResponseEntity.ok(new CsrfResponse(token.getHeaderName(), token.getToken()));
    }

    @Override
    public ResponseEntity<DeletionResult> deleteAccount() {
        boolean done = deletion.deleteCurrent();
        if (done) {
            var session = request.getSession(false);
            if (session != null) session.invalidate();
        }
        return ResponseEntity.status(done ? 200 : 202).body(new DeletionResult(done
                ? DeletionResult.StateEnum.COMPLETED : DeletionResult.StateEnum.PENDING));
    }
}
