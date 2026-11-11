# Development

No application source/build or Compose configuration is tracked yet. Local untracked build artifacts are not a supported application. Add actual setup and test commands here when the first service is introduced.

## Working agreement

Keep task scope, acceptance criteria, failure cases, and test evidence in an issue or PR. Routine tasks do not need new documents. Update existing docs only when lasting product behavior, architecture, or developer commands change. Significant architectural choices use a Proposed row in [decisions](decisions.md); only the user accepts it. Do not add tooling or infrastructure merely because it appears in the target architecture.

Planned source locations: `services/core-api`, `services/agent-service`, `web`, and later `services/mcp-server`. Contracts belong to their owning service; generate clients instead of copying DTOs. Do not create empty directories or speculative modules.

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

[GitHub CI](../.github/workflows/ci.yml) runs on PRs and pushes to main. There are currently no application tests: its single `CI gate` job reports that state and checks patch whitespace. This is not evidence of passing application tests. Add explicit real service build/test commands in the same change that introduces each service; missing builds or failed tests must fail CI. No custom repository-checker scripts, architecture-validator downloads, deployments, or paid API calls are needed. Branch-protection settings have not been checked or changed.

For documentation changes, inspect local links and consistency and run `git diff --check`.

## What belongs outside the repository

Optional Wiki material: learning notes, framework tutorials, detailed portfolio narrative, historical review reports, and release planning commentary. GitHub issues can hold the backlog. These are not development prerequisites; do not duplicate authoritative rules there. Nothing has been published to a Wiki by this cleanup. Git history retains removed material.
