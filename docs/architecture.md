# Architecture

This is the intended architecture. No application source/build or Compose configuration is tracked yet. The existing [C4 workspace](c4/workspace.dsl) is a target view, not a deployment inventory. Reasons and decision history live in the [decision table](decisions.md).

## Components and ownership

| Component | Responsibility | Planned technology |
|---|---|---|
| Core API | Sole writer of domain data; authorization and transactions | Java 21, Spring Boot, Spring Modulith, Maven |
| Web | Responsive social and training UI | React, strict TypeScript, Vite |
| Agent service | Validated AI proposals; no domain database access | Python, FastAPI, Anthropic behind a provider interface |
| Database | Relational data, workout JSONB, embeddings when needed | PostgreSQL, pgvector, Flyway |
| Identity provider | OIDC credentials and provider identities | Keycloak; isolated provider database/user |
| Broker | Cross-service asynchronous work when implemented | RabbitMQ with transactional outbox |
| MCP (later) | Permissioned reads through the core API | TypeScript, Node.js |

## Core boundaries

Core packages under `com.syla.core` are `identity`, `workouts`, `progress`, `social`, `gyms`, `wearables`, and `privacy`. Add modules as behavior needs them. Each owns its same-named PostgreSQL schema. Public types live at the module root; implementation lives in `internal`. No cross-module internal access, SQL joins, foreign keys, or JPA relations. Verify boundaries with Spring Modulith tests. Use public APIs for queries and events for cross-module side effects.

Only the core saves confirmed domain changes. Python never writes domain data. HTTP/OpenAPI carries synchronous requests; documented event schemas carry asynchronous facts. Use timeouts for external calls. Cross-service events use the Modulith publication registry/outbox, idempotent consumers, bounded retries, and dead-letter handling. Do not add speculative consumers.

## Identity and privacy

Use Keycloak OIDC Authorization Code + PKCE through Spring Security in the core. Map `(issuer, subject)` to an athlete, never email. Tokens stay server-side. Web uses a same-origin, HttpOnly, host-only session cookie (Secure over HTTPS), CSRF protection, state/nonce checks, session rotation, and validated redirect destinations. Start with in-memory core sessions: restart requires login, not loss of workout data. Initial defaults are 30-minute idle / 12-hour absolute lifetime. API expiry returns JSON 401; failed CSRF returns 403. Logout clears core sessions and private UI state and integrates provider logout. Verify issuer/callback routing from browser and containers.

Enforce ownership and audience rules in the core. Authentication is separate from consent. Support export and retryable account deletion across implemented modules and the identity provider. Block mutations before cleanup; concurrent requests, delayed jobs, or a stale provider session must not restore/reprovision a deleted account. Registration, provider revocation, deletion/re-registration retention, and production identity configuration still need concrete implementation decisions.

No secrets, tokens, workout payloads, or health values in logs/traces. Minimize identifiers. Whiteboard photos are resized to about 1600 px, JPEG-compressed, and stripped of EXIF in the browser; process in memory and discard everywhere. Never persist or log images. Save only reviewed structured data. Permissioned, de-identified evaluation fixtures are separate from production data.

## Shared training

A gym class is a template; each attendee owns an independent workout snapshot. Class edits preserve existing attempts. Public gym classes/results are visible to signed-in users; private classes/results require membership. Member lists always require membership. Private joining uses approved requests or expiring/revocable invites. Admins manage settings; becoming public requires confirmation. Hidden scores show attendance but are excluded from ranking. Rank RX/scaled separately with explicit comparability rules. Last-admin departure transfers administration to the longest-standing member. Deletion removes personal memberships/attendance/results; shared classes remain without publisher identity. Detailed empty-gym and ranking edge cases remain open.

## Runtime and open decisions

Use environment configuration, no repository secrets, and Docker Compose for the eventual local stack. AWS is the later deployment target; isolate AWS-specific APIs behind interfaces. Add infrastructure only when the implemented slice uses it. Agent framework, Oura sync model/terms, production sessions, hosting/region, and retention policies remain open. Accepted choices describe direction, not proof that infrastructure exists.
