# ADR-0015: Personal logging write safety and deletion

- Status: Accepted
- Accepted by the user: 2026-11-10
- Date: 2026-11-10

## Context

M1 needs safe retries, stale-edit checks, private records, consent, export, and deletion. The accepted module/schema boundaries remain unchanged. A request-time account check alone cannot stop a concurrent save after account deletion.

## Decision

- Keep JPA/Hibernate with Flyway. Use a transaction-scoped row lock on the actor's identity record through a public identity query. All training reads/writes, consent changes, export, and deletion take this lock first. Other modules never query identity tables directly. This serializes requests for one athlete; different athletes proceed in parallel.
- Save one workout atomically. Use a client-generated UUIDv7 idempotency key, whose immutable embedded timestamp defines the retry window, bound to actor and canonical payload hash. Keys are valid for 24 hours, allowing at most 5 minutes of future clock skew. Same key/payload replays the current record only if unchanged; changed/deleted outcomes return 409. Reject expired key timestamps even if a key was purged, so an old key cannot create a new record. Do not retain raw request bodies in the key table. Purge keys after the retry window; account deletion purges them immediately.
- Use numeric revisions for replacement/deletion. Require `If-Match`; stale revisions return 409, missing ones 428. Missing/nonowned IDs return the same 404. Repeating a deletion is safe and never restores data.
- Consent v1 covers storing private training history for this local demo. Consent is explicit, versioned, timestamped, and never inferred from login. Withdrawal blocks workout reads/writes; export and deletion remain available. Real-user consent wording and lawful basis need separate review before launch.
- Deletion first marks the account DELETING and purges training/consent/retry data under the same actor lock via synchronous module events. New training requests are blocked. Then revoke/delete the Keycloak user through a server-side adapter. Failed provider cleanup returns a pending result and can be retried; it must not report completed deletion.
- Keep only the account UUID, issuer/subject, deletion state, and timestamp as a tombstone for 30 days after provider deletion succeeds. Its purpose is to block stale sessions from restoring a deleted account. No workout, name, email, or consent payload remains. Purge the tombstone after 30 days. API requests never provision accounts; provisioning happens only after a fresh verified OIDC login. No automatic re-registration is allowed while a tombstone exists.
- Keep one local core instance and server sessions per ADR-0012. Every API request checks current account state. No Redis, distributed lock, or new service is added.

## Alternatives considered

- An account check before starting a workout transaction leaves a deletion race.
- Cross-schema foreign keys/joins break ADR-0002/0004.
- Permanent retry payload copies and permanent account tombstones retain more personal data.
- A distributed lock adds a dependency without a multi-instance need.

## Consequences

Per-athlete writes are serialized and the retry window is explicit. Identity rows become a short-lived coordination point. The client must keep a UUIDv7 create key for retries and send revisions on edits/deletes. Provider failure is visible. The 30-day tombstone is pseudonymous personal data and must be included in the data inventory, export, and retention checks.

## Verification

Real PostgreSQL race tests: same-key creates, stale edits, delete/replay, consent withdrawal, save versus account deletion. API tests: two accounts, CSRF, no-store headers, missing/nonowned IDs, export isolation, and retry after provider failure. Verify tombstone expiry and no re-provisioning from old sessions.

## Open questions

The user accepted this protocol and its local-demo retention defaults on 2026-11-10. This does not constitute approval for real-user launch.
