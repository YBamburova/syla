# Development

The implemented foundation is a Java 21 / Spring Boot API in `services/core-api` and a React/Vite frontend in `web/`, with a generated TypeScript Fetch client. It includes PostgreSQL/Flyway, Keycloak OIDC sessions, and private manual activity logging. Node.js is pinned to 24.22.2; dependencies and the OpenAPI generator are pinned in Maven/npm manifests.

## Working agreement

Keep task scope, acceptance criteria, failure cases, and test evidence in an issue or PR. Routine tasks do not need new documents. Update existing docs only when lasting product behavior, architecture, or developer commands change. Significant architectural choices use a Proposed row in [decisions](decisions.md); only the user accepts it. Do not add tooling or infrastructure merely because it appears in the target architecture.

Existing source locations: `services/core-api` and `web`. Future services belong in `services/agent-service` and later `services/mcp-server`. Contracts belong to their owning service; generate clients instead of copying DTOs. Do not create empty directories or speculative modules.

## Code and verification

Use clear names, small functions, precise types, immutable values where practical, and boundary validation. Explain non-obvious reasons in comments. Handle errors explicitly, parameterize SQL, and never log secrets or health data. UTC instants are distinct from explicitly stored training dates/time zones. Pin dependencies and commit lockfiles; justify new dependencies in the PR.

| Area | Conventions and intended checks |
|---|---|
| Java | Java 21; records/value types, BigDecimal loads, Duration/Instant; constructor injection, thin controllers, transactions in application services, internal JPA entities, immutable merged Flyway migrations; `/api/v1` and Problem Details. Maven Wrapper verify must run JUnit/AssertJ, Modulith verification, and configured integration tests with real dependencies via Testcontainers. |
| Python | Version pinned in pyproject; uv lockfile, Ruff, strict mypy, typed functions, Pydantic boundary/LLM validation; async I/O without blocking handlers. No database. pytest mocks the provider boundary. Parser/model/prompt changes need evaluation evidence against the agreed dataset and targets. |
| TypeScript/web | Strict types, narrow unknown instead of any; generated OpenAPI client, React hooks, TanStack Query server state, mobile layouts, labels/keyboard/focus and accessible errors. ESLint/Prettier, typecheck, Vitest single-run, production build; Playwright for real user flows. |

Test meaningful behavior and regressions, including ownership, privacy, invalid inputs, retries/conflicts, and external failures where relevant. Manual logging works without AI/Oura. Run bounded checks for the implemented change; report missing checks honestly. Do not add tests that merely mirror implementation.

## Git and CI

Use short-lived branches and PRs into main; squash merge, Conventional Commit titles, signed-off commits (`git commit -s`), and platform SemVer tags when authorized. Preserve existing branch names; Codex uses `codex/`. Never commit unless asked, push to main, bypass hooks, or run destructive Git operations without explicit permission. Stage specific files and inspect the diff.

[GitHub CI](../.github/workflows/ci.yml) has one `CI gate` job that builds/tests the API, verifies module boundaries and the exported OpenAPI contract, regenerates/checks the frontend client, runs frontend tests/build, checks desktop/mobile browser-to-API and Swagger flows, and validates Compose configuration. New service manifests must come with explicit real checks. No paid API calls, automatic deployments, or custom Python CI scripts. Hosted execution and branch-protection settings are not verified by local checks.

## Run and verify

Startup commands and local URLs are in [README](../README.md). API requests from Vite and packaged nginx use the same browser origin; no permissive CORS configuration is needed. Swagger exposes schemas publicly, not athlete records. Interactive API calls still require sign-in and a CSRF token for mutations. All activity operations are owner-scoped. There is no login bypass or shared athlete password. Vite and nginx proxy `/oauth2`, `/login`, and `/logout` as well as `/api` and Swagger. Local development uses HTTP cookies only on localhost; production must set Secure cookies and hardened TLS/provider settings.

From `services/core-api` (Java 21 and a running Docker engine):

```sh
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress generate-sources -Pgenerate-web-client
```

From `web` (Node 24.22.2):

```sh
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

The browser tests require the API running at port 8080; Playwright starts/stops Vite itself on 5173. Do not run Compose on the same ports while using these commands. Browser tests register synthetic users through real Keycloak, consent, log all four sports, reload/verify civil dates, edit/delete activities, export, and delete accounts. Desktop runs in Los Angeles and mobile in Kyiv to exercise date handling. Screenshots go to ignored `web/test-results/`. No tests call cloud/LLM/wearable services.

The API suite includes deterministic unit tests plus Failsafe `*IT` tests using a real Testcontainers PostgreSQL database. It verifies the domain, persistence, units/pool metrics, owner isolation, consent/CSRF, idempotency, stale revisions, export, retryable deletion, module boundaries, Swagger, and contract drift. Identity cleanup is mocked only in the integration failure test; the full browser flow uses real Keycloak cleanup. It exports the actual OpenAPI to `services/core-api/target/openapi.json` and compares it structurally with committed `services/core-api/openapi.json`.

After an intentional API change, run the API tests, review the resulting contract difference, and copy `target/openapi.json` to `openapi.json`. The drift test is expected to fail until this reviewed snapshot is updated. Then regenerate the web client and rerun verification. Generated code lives only in `web/src/api/generated`; generation replaces this directory to remove obsolete types. Never place handwritten code there. Generator bookkeeping is ignored; generated TypeScript is committed. CI rejects uncommitted generated differences, including new files.

Container configuration can be checked with `docker compose config --quiet` (`docker-compose` with standalone Compose). Startup and generated `.env` secrets are documented in README. Core and Keycloak have separate database users/databases on one PostgreSQL engine; the core is not a database superuser and has no provider-database access. Keycloak 26.8.2 requires the `kc-admin-api` default client scope as well as management roles ([upgrade guidance](https://www.keycloak.org/docs/26.8.2/upgrading/)); the realm import assigns this scope to the lifecycle client. Keycloak's local lifecycle service account has `manage-users` to delete provider accounts; restrict/harden this administrative adapter before production. Realm imports are applied only to a new realm; changing the realm JSON does not silently overwrite existing provider state.

On macOS/Colima, Testcontainers may need `DOCKER_HOST=unix://$HOME/.colima/syla-dev/docker.sock` and `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`. CI uses the runner's Docker socket. Tests use synthetic data and ordinary PR checks call no paid services. CI creates random local secrets, starts PostgreSQL/Keycloak, and cleans up its containers even on failure. Never log request payloads, provider responses/tokens, or generated secrets.

Local verification on 2026-11-11: 14 Java tests (2 domain + 12 PostgreSQL integration), 4 frontend tests, production build, both container image builds, complete Compose startup, and two desktop/mobile browser flows through the packaged nginx/API/PostgreSQL/Keycloak stack passed. Browser coverage includes sport filtering, all four sports, reload/date preservation across Kyiv and Los Angeles zones, editing, deletion, export, and real provider account deletion.

Review verification on 2026-11-15: fixed downloaded-export calendar-date serialization and preserved the original edit revision/input across background refreshes. Added a frontend regression test and actual browser-download assertions.

For documentation changes, inspect local links/consistency and run `git diff --check`. No hosted CI result, measured logging-speed target, or human usability-study result is claimed by local verification.

## What belongs outside the repository

Optional Wiki material: learning notes, framework tutorials, detailed portfolio narrative, historical review reports, and release planning commentary. GitHub issues can hold the backlog. These are not development prerequisites; do not duplicate authoritative rules there. Nothing has been published to a Wiki by this cleanup. Git history retains removed material.
