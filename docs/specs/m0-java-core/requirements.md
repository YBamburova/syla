# M0 Java core requirements

Scope: the first Java foundation slice of [M0](../../milestones.md). Implements [ADR-0002](../../adr/0002-modular-monolith-core.md), [ADR-0008](../../adr/0008-docker-compose-local-aws-target.md), and [ADR-0014](../../adr/0014-staged-github-actions-ci.md).

- Java 21 and the Maven Wrapper build a runnable Spring Boot JAR with one `./mvnw clean verify` command. Pin tools and dependency BOMs; use no snapshots for dependencies.
- Anonymous `GET /api/v1/health` returns HTTP 200 and only `{"status":"UP"}`. This is process health, not database or login readiness. It reads and writes no personal data and needs no credentials.
- Unknown routes return 404; POST to health returns 405. Failed startup or a bad health response fails the smoke test within a fixed time and stops the child process.
- Modulith detects the technical health module and verifies all present module boundaries. No domain modules are added just to fill the layout.
- `verify` runs both the boundary test and a packaged-JAR HTTP test. Repeat clean builds on the same pinned toolchain produce the same JAR hash.
- Core CI runs on every PR/main push and is a required dependency of CI gate. Failed/cancelled/skipped/missing core results fail the gate. Removing the core POM or wrapper fails repository checks.
- Document direct JAR startup and core-only Compose startup, config, tool versions, checks, and gaps. No paid service or real user data is needed.

No persistence, migrations, Keycloak login, web, Python, workout, AI, social, or Oura work. Those stay in later M0/M1 slices. Export, consent, and deletion become relevant when personal data exists; this slice stores none.
