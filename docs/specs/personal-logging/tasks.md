# Personal logging — implementation tasks

Status: Initial product defaults approved; remaining tasks are planned. Complete specifications and decision gates before dependent code. Links: [requirements](requirements.md), [design](design.md), [M0/M1 milestones](../../milestones.md), [review findings](../../architecture/review-2026-11-04.md).

## A. Decisions and concrete contracts

Accepted setup decisions: [authentication ADR-0012](../../adr/0012-oidc-login-and-server-sessions.md) and [web tooling ADR-0013](../../adr/0013-web-application-tooling.md). A2/A3 retain implementation prerequisites and the separate CI design; approval of the setup does not resolve every lifecycle detail.

- [x] A1. User approved the five initial defaults on 2026-11-04: strength/AMRAP/For Time, one section, private history, optional unscored logging, and today/past training dates. Detailed validation and technical decisions remain draft; no ADR was accepted. Covers the initial product boundary for R1/R2.
- [ ] A2. Research authentication options against local development and later deployment; draft an ADR with recommendation, browser protections, identity mapping, account lifecycle, and unresolved questions. User acceptance is required. Covers R2/R4.
- [ ] A3. Research and propose web tooling and CI decisions. Include generated API clients and browser verification. Accept decisions before adding dependent frameworks.
- [ ] A4. Define exact discriminated schemas, field limits, movement catalog, units/precision, error codes, and W1–W3 valid/invalid fixtures. Document normalized AMRAP scoring and scaling constraints. Covers PL-02–06/R1.
- [ ] A5. Specify runtime transactions, revision preconditions, idempotency retry/expiry lifecycle, replay after edits/deletes, and concurrent account deletion. Finalize internal event contracts. Covers PL-07–08/R3.
- [ ] A6. Inventory personal data and consent purposes; define export, deletion barrier/retries, consent withdrawal, operational retention, and threat scenarios. Review negative access cases. Covers PL-01/10/R2/R4.

## B. Foundation dependencies — M0

- [ ] B1. Establish the authorized Git/GitHub baseline and review process; never commit or publish without authorization.
- [ ] B2. Implement approved core/web/database foundation, migrations, module verification, and generated contract workflow. Add accepted platform service skeletons as scoped in M0; do not create unused business features.
- [ ] B3. Document and demonstrate clean startup and bounded checks; CI must execute real builds/tests. Update actual versus planned architecture/deployment views.

## C. Core personal logging

- [ ] C1. Implement authenticated actor mapping and account/consent eligibility checks from A2/A6. Verify unauthenticated/nonowner rejection and deletion races. Covers PL-01/10.
- [ ] C2. Implement strength, AMRAP, For Time, scaling, metadata, and unscored domain validation with W1–W3 tests. Covers PL-02–06.
- [ ] C3. Add Flyway persistence and owner-scoped create/list/detail with atomic save and idempotency. Integration tests include concurrent and lost-response retries, same-key mismatches, and safe repeated same-content workouts. Covers PL-07.
- [ ] C4. Add revision-protected replacement and deletion with change events. Test conflicts, repeats, stale delete, and create replay after edit/delete. Covers PL-08.
- [ ] C5. Implement privacy orchestration for the complete implemented slice. Verify export isolation, partial cleanup retry, no post-deletion restoration, and defined retained-data policy. Covers PL-10.
- [ ] C6. Publish OpenAPI and generate the web client/types; verify no client hand-copies domain payload contracts. Inspect logs/traces for excluded payloads.

## D. User flow

- [ ] D1. Implement sign-in integration, history/detail, mobile form, explicit units/scaling, unsaved-change handling, and unscored display. Covers PL-02–06/09.
- [ ] D2. Implement save/retry, field errors, session expiry, edit conflict, and delete confirmation states. Verify retained input and no silent duplicate creation. Covers PL-07–09.
- [ ] D3. Implement settings consent/export/account deletion actions with the separate confirmations and states required by A6. Covers PL-10.

## E. Demonstration and M1 gate

- [ ] E1. Run domain, real PostgreSQL integration, module boundary, and generated-contract checks. Verify every PL requirement has evidence, including unauthorized and failure cases.
- [ ] E2. Run browser scenarios: W1, W2 RX/scaled, W3 completed/capped; save/reload/restart, edit, delete, and account export/deletion. Include keyboard/mobile/error checks and a second account's denied access.
- [ ] E3. Demonstrate logging with AI/Oura unavailable; measure manual entry time and record conditions, observed results, and limitations against the <30 s goal.
- [ ] E4. Update README, runtime/building-block views, data inventory, and milestone evidence. Review remaining issues with the user before marking M1 done. No code generation or passing unit tests alone satisfies the milestone.

These are implementation slices, not a requirement to put each checkbox into a separate PR. Group coherent work while keeping reviews small. Resolve failed checks before declaring the associated task complete.

## Backend checkpoint 2026-11-10

- [x] User chose OpenAPI YAML → generated Java interfaces/record DTOs.
- [x] User accepted ADR-0015 for write safety and deletion.
- [x] Add pinned JPA/Hibernate, Flyway PostgreSQL support, pgvector Compose, and isolated Keycloak persistence.
- [x] Add generated health, account, privacy, and workout contracts with Swagger UI.
- [x] Add backend CRUD, owner checks, typed scores, consent, export, revision/retry checks, and deletion code.
- [x] Unit/domain/retry/module tests pass (11 Java tests); repository checker tests pass (11 Python tests).
- [ ] Final provider cleanup check remains failing: Keycloak admin cleanup returns HTTP 401 and deletion stays PENDING. Final run: 11 Java unit/module tests passed; 5 of 6 real PostgreSQL/Keycloak integration tests passed. Login, CRUD, score checks, retry/revision checks, and consent/export passed. Keep the failing deletion test; this stage is not ready to merge as complete M1.
- [ ] Full Compose startup, frontend/browser demo, and hosted CI remain unverified.

No full M1 completion or production readiness is claimed. Earlier A–F tasks describe the complete milestone; this checkpoint records only the backend slice.
