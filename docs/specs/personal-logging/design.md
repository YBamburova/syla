# Personal logging — design

Status: Proposed design, 2026-11-04. See [requirements](requirements.md) and [tasks](tasks.md). No code, final OpenAPI, migration, or new infrastructure is introduced here.

## Responsibilities

| Area | Responsibility |
|---|---|
| React web | Format-specific form, accessible validation, explicit save/retry state, history/detail, settings actions; generated API client |
| `identity` | Resolve the authenticated actor, account state, profile and authentication integration; technology pending ADR |
| `workouts` | Domain validation, owner-scoped commands/queries, session persistence, revisions, save idempotency, internal change events |
| `privacy` | Applicable consent checks and export/deletion orchestration through public module APIs |

The core API remains the only writer. No cross-schema joins, foreign keys, or JPA relations. The workout mutation's account/consent checks and account-deletion barrier need an explicit concurrency design through public module contracts; a pre-request check alone is insufficient to prevent writes racing deletion.

## Domain and storage outline

One session aggregate owns one section for M1, preserving the [model](../../workout-model.md) boundary for multi-section support later. Format and result types are discriminated structures rather than arbitrary maps. A scored/unscored discriminator prohibits incompatible payload combinations.

Proposed relational session fields: identity, owner reference, training date/zone, timestamps, revision, optional title/notes. Section fields include identity, parent session, order, and format. Validated versioned JSONB can contain format-specific prescription, actual configuration, and result. Fields needed for filtering/indexing remain relational under ADR-0004. Exact schema and bounds remain a contract-design task; do not build future leaderboard indexes now.

Strength results map to prescribed set identities. AMRAP substitutions map to occurrence identities in one repeatable round; For Time can map across its finite ordered rounds. The specification supports one stable performed configuration within each scored conditioning section, not chronological mid-attempt substitutions.

W1–W3 become deterministic fixtures in the eventual tests. They are not prefilled RX defaults that silently assert what a user lifted. Movement selection starts with a small explicit catalog covering these fixtures, including ring rows and bodyweight movements; movement creation and fuzzy/vector matching are outside this slice. Unsupported movement entry must be explained, not silently mapped to a different exercise.

## Candidate HTTP surface

Paths are proposals under the accepted API major version; confirm details in generated OpenAPI before clients are built.

| Operation | Candidate endpoint | Behavior |
|---|---|---|
| List | `GET /api/v1/workouts` | Actor-scoped, bounded cursor pagination |
| Create | `POST /api/v1/workouts` | Validated session, idempotency key, server-assigned ownership |
| Detail | `GET /api/v1/workouts/{id}` | Actor-scoped aggregate and revision |
| Replace | `PUT /api/v1/workouts/{id}` | Full validated replacement with expected revision |
| Delete | `DELETE /api/v1/workouts/{id}` | Expected revision and safe repeat semantics |

Errors use RFC 9457 Problem Details as required by repository conventions. Define stable codes/field paths for validation, idempotency conflict, stale revision, and inactive-account/consent failure. Proposed validation status is 400; stale revision/idempotency mismatch is 409; missing/nonowned resource is 404. Authentication and browser-session protections depend on the authentication ADR. Export, consent, and account-deletion endpoint contracts belong to the privacy design and must be implemented for the M1 gate.

## Save and retry sequence

1. Browser creates one opaque idempotency key for a logical create operation and keeps it during uncertain retries. If the user changes an uncertain request, resolve its outcome first instead of reusing its key with new content.
2. Core authenticates, resolves account eligibility/consent, and validates the complete payload. Invalid requests write no workout.
3. Within the workout transaction, bind the key to actor and canonical payload fingerprint, save the aggregate, and record the successful outcome. Concurrent same-key requests converge to one save through database constraints/transaction handling.
4. Same actor/key/payload returns the same saved outcome; same key with different content conflicts. New logical attempts receive new keys. Define a retry window and safe replay response before code; an expired key must not be silently accepted as a fresh create. Do not retain a second raw personal-data payload merely for idempotency.
5. Register applicable internal events transactionally. Respond only after commit. Lost response can be retried with the same key; database failure produces no partial aggregate or false success.

Delete/edit must invalidate or supersede replay outcomes safely so an old create retry cannot resurrect deleted data or return an outdated result as current. Account deletion cleans actor-associated idempotency records under the privacy policy. This lifecycle is a design gate, not an assumed property of a header.

## Update and deletion

Replace checks ownership and expected revision in the mutation transaction, retaining stable identities. A version conflict returns no mutation and lets the user reload or compare their unsaved form. Successful edits/deletes register versioned past-tense facts for later consumers. Event payloads minimize personal data; retention and replay handling are part of privacy/runtime design.

Workout deletion is distinct from account deletion. Define repeated delete behavior without leaking record existence; a removed session cannot be recreated by an old retry. Account deletion uses an account mutation barrier and idempotent module cleanup; exactly how barriers and module transactions coordinate must be resolved before implementing privacy endpoints.

No cross-service async work is required for manual logging. RabbitMQ remains part of the accepted platform; no dummy insight consumer is introduced to exercise it. Internal events obey Spring Modulith boundaries and durable publication decisions. Externalization, if later needed, follows ADR-0006.

## UI states and data handling

History: loading, empty, populated, failed/retry. Editor: editing, invalid, saving, uncertain/retry, authentication expired, revision conflict, saved. Delete: confirmation, pending, failed/retry, removed. Disable redundant submits while pending; server idempotency remains the correctness mechanism.

Keep a failed form in memory while the page remains open; do not persist it in local storage. Session expiry must explain possible loss if reauthentication reloads the page. Authenticated response cache directives and fetch behavior must be defined in the security design. Avoid promising offline support.

## Verification and remaining gates

Domain tests cover normalized scores, scaling, missing versus zero, capped/completed exclusivity, and W1–W3. PostgreSQL integration tests cover atomicity, concurrent retries, revisions, owner scoping, and deletion/replay interactions. Module verification tests enforce boundaries. Browser tests cover a real save/reload/edit/delete flow and accessible errors, using the chosen tool after tooling approval. Privacy tests exercise the account lifecycle, not just endpoint happy paths.

Before implementation: finalize product defaults from requirements; accept authentication/tooling/CI decisions; specify schemas/limits and movement catalog; settle idempotency expiration and deletion races; document runtime sequences and the privacy data inventory/threat model. No particular authentication provider or additional test framework is selected by this draft.

## Backend implementation 2026-11-10

The user chose OpenAPI YAML → generated Spring interfaces and Java record DTOs. Contracts live in `services/core-api/src/main/openapi`; generated code stays in `target`. Each module owns its generated public contract. Springdoc supplies Swagger UI; the YAML is authoritative.

Accepted [ADR-0015](../../adr/0015-personal-logging-write-safety.md) supplies actor locking, UUIDv7 retry expiry, and account deletion. JPA/Hibernate stores relational ownership/date/revision metadata and versioned typed section JSONB. Flyway owns schema changes; Hibernate only validates. Native SQL is limited to bound identity registration and indexed cursor queries within the owning schema. No cross-schema joins/foreign keys exist.

Account provisioning happens only after verified OIDC login, using issuer/subject. Every service query takes the actor lock and checks current state. Workouts then query the public consent API. Identity emits a synchronous deletion event; privacy and workouts purge their own rows in that transaction. Provider cleanup follows commit and retries while DELETING. A 30-day minimal identity tombstone blocks old sessions. Export uses a synchronous in-memory query event so privacy does not depend back on workouts and create a module cycle.

CRUD, consent, export, and account routes are described in the YAML and [core README](../../../services/core-api/README.md). Strength/AMRAP/For Time are typed, with original prescription and actual work kept separate. JSON has a 256-KiB limit; names, input values, notes, and tokens are omitted from error diagnostics. There are no external training consumers yet; internal metadata events have no durable derived-data subscriber or RabbitMQ externalization.

The backend is under final checks. It does not complete M1: React, browser flows, logging-time measurement, and public-launch privacy/auth policy remain open.
