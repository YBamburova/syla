# Architecture

The Java core and React frontend implement private manual running/swimming/cycling/general workout logs, Keycloak OIDC sessions, recorded consent, PostgreSQL/Flyway persistence, owner-scoped CRUD, export, and retryable account deletion. Automatic profiles, athlete search, follow approvals, blocking and follower-only activity summaries are also implemented. OpenAPI generates the TypeScript client. The [C4 workspace](c4/workspace.dsl) remains the broader target; AI, broker workflows, CrossFit formats, social feeds, and wearables are not implemented. Reasons and history live in the [decision table](decisions.md).

## Components and ownership

| Component | Responsibility | Technology / target |
|---|---|---|
| Core API | Sole writer of domain data; authorization and transactions | Java 21, Spring Boot, Spring Modulith, Maven |
| Web | Responsive social and training UI | React, strict TypeScript, Vite |
| Agent service | Validated AI proposals; no domain database access | Python, FastAPI, Anthropic behind a provider interface |
| Database | Relational data, workout JSONB, embeddings when needed | PostgreSQL, pgvector, Flyway |
| Identity provider | OIDC credentials and provider identities | Keycloak; isolated provider database/user |
| Broker | Cross-service asynchronous work when implemented | RabbitMQ with transactional outbox |
| MCP (later) | Permissioned reads through the core API | TypeScript, Node.js |

The frontend stays in `web/` in the same GitHub repository as `services/core-api`, as already selected in decisions 0003/0013. This keeps contract and UI changes in one review. Java DTOs generate the OpenAPI schema through springdoc; OpenAPI Generator creates the TypeScript client. Persistence entities stay internal and separate from transport DTOs. The implemented modules are `identity`, `social`, `workouts`, `privacy`, and `platform`. Account and workout schemas have no cross-module foreign keys. Stable workout identity/owner/date/sport/duration fields are relational; optional sport metrics use validated JSONB. A locked account row serializes mutations with deletion and duplicate submissions.

## Core boundaries

Core packages under `com.syla.core` are `identity`, `workouts`, `progress`, `social`, `gyms`, `wearables`, and `privacy`. Add modules as behavior needs them. Each owns its same-named PostgreSQL schema. Public types live at the module root; implementation lives in `internal`. No cross-module internal access, SQL joins, foreign keys, or JPA relations. Verify boundaries with Spring Modulith tests. Use public APIs for queries and events for cross-module side effects.

Only the core saves confirmed domain changes. Python never writes domain data. HTTP/OpenAPI carries synchronous requests; documented event schemas carry asynchronous facts. Use timeouts for external calls. Cross-service events use the Modulith publication registry/outbox, idempotent consumers, bounded retries, and dead-letter handling. Do not add speculative consumers.

## Identity and privacy

Use Keycloak OIDC Authorization Code + PKCE through Spring Security in the core. Map `(issuer, subject)` to an athlete, never email. Tokens stay server-side. Web uses a same-origin, HttpOnly, host-only session cookie (Secure over HTTPS), CSRF protection, state/nonce checks, session rotation, and validated redirect destinations. Start with in-memory core sessions: restart requires login, not loss of workout data. Initial defaults are 30-minute idle / 12-hour absolute lifetime. API expiry returns JSON 401; failed CSRF returns 403. Logout clears core sessions and private UI state and integrates provider logout. Verify issuer/callback routing from browser and containers.

Enforce ownership and audience rules in the core. Authentication is separate from consent. Support export and retryable account deletion across implemented modules and the identity provider. Block mutations before cleanup; concurrent requests, delayed jobs, or a stale provider session must not restore/reprovision a deleted account. Local registration is hosted by Keycloak; email verification/delivery is disabled only in the local realm. Provider account deletion uses a server-side management client. Successful deletion retains only a hash-based identity blocker for 24–25 hours to outlive all 12-hour app sessions, then purges it. Production identity hardening and provider-session revocation behavior still require review before real users.

No secrets, tokens, workout payloads, or health values in logs/traces. Minimize identifiers. Whiteboard photos are resized to about 1600 px, JPEG-compressed, and stripped of EXIF in the browser; process in memory and discard everywhere. Never persist or log images. Save only reviewed structured data. Permissioned, de-identified evaluation fixtures are separate from production data.

## Athlete social access

Identity owns registration names and athlete profiles. Session/consent synchronizes the authenticated OIDC username and given/family names and initializes missing profiles, defaulting to approval-required following. Existing profile edits are never overwritten; deletion clears these registration fields as well. No Keycloak database reads or bulk provider-account imports are used. Social owns follow requests, accepted relationships and directed blocks. Both use bounded, parameterized PostgreSQL queries without new dependencies. Workouts reads permissions through the public social API; social never queries workout tables. Privacy orchestrates export and deletion through public module APIs.

Relationship mutations lock both active identity rows in stable UUID order. This serializes reciprocal requests, blocking and deletion without a global lock. Shared activity reads use the same locks before checking live permissions and reading data. All new and migrated activities are PRIVATE. Changing to FOLLOWERS is explicit and revision-guarded; content edits preserve the audience. Approved followers receive a separate allowlisted DTO containing title, sport, training date, duration, canonical distance and calculated speed/pace. It excludes notes, health metrics, equipment, effort, exact start time and time zone. Nonpermitted direct IDs return 404; access is checked on each request. No copied feed, cache infrastructure or asynchronous consumers are introduced.

## Shared training

A gym class is a template; each attendee owns an independent workout snapshot. Class edits preserve existing attempts. Public gym classes/results are visible to signed-in users; private classes/results require membership. Member lists always require membership. Private joining uses approved requests or expiring/revocable invites. Admins manage settings; becoming public requires confirmation. Hidden scores show attendance but are excluded from ranking. Rank RX/scaled separately with explicit comparability rules. Last-admin departure transfers administration to the longest-standing member. Deletion removes personal memberships/attendance/results; shared classes remain without publisher identity. Detailed empty-gym and ranking edge cases remain open.

## Runtime and open decisions

Use environment configuration, no repository secrets, and Docker Compose for the eventual local stack. AWS is the later deployment target; isolate AWS-specific APIs behind interfaces. Add infrastructure only when the implemented slice uses it. Agent framework, Oura sync model/terms, production sessions, hosting/region, and retention policies remain open. Accepted choices describe direction, not proof that infrastructure exists.
