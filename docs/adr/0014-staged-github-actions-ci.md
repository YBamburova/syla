# ADR-0014: Staged GitHub Actions CI with one required gate

- Status: Accepted
- Accepted by the user: 2026-11-05
- Date: 2026-11-05

## Context

ADR-0011 selects GitHub Actions and PR-based changes. The repository currently contains documentation only. Java, Python, web, containers, and generated clients will arrive during M0/M1. CI must check real work, remain usable by a solo maintainer, and run without production credentials. Acceptance of this ADR is separate from implementation or publishing permission.

## Decision

Use one main CI workflow on GitHub-hosted Ubuntu runners, triggered by every pull request targeting `main` and every push to `main`. Include PR `edited` events for title validation alongside opened/synchronize/reopened events. No top-level path filters and no `[skip ci]` convention. Do not duplicate PR builds with pushes to every feature branch.

Use one stable required job named `CI gate`. It runs with `always()` after its dependencies and succeeds only when every applicable check succeeds. It must reject failed/cancelled jobs and unexpected skips. Inapplicable work must be explicitly identified by a checked execution plan, not inferred from a missing manifest. Start without change-based optimization: once a component is introduced, check it on every PR. Optimize only if measured CI cost warrants it.

### Staged checks

| Stage/job | Checks | When enabled |
|---|---|---|
| Repository | Local Markdown links including anchors, no escaped paths outside the checkout, known source/config secret patterns, PR title Conventional Commits shape | First CI PR |
| Architecture | Parse/validate the Structurizr DSL with a pinned CLI; detect invalid references/syntax | First CI PR |
| Core | Maven Wrapper verify: compile, unit tests, configured integration tests, Spring Modulith verification, migrations against real PostgreSQL | Same PR that introduces the Java core |
| Agent | Locked uv sync, Ruff lint/format check, mypy strict, pytest | Same PR that introduces Python service |
| Web | npm ci, ESLint/Prettier checks, typecheck, Vitest single run, production build | Same PR that introduces web |
| Contracts | Generate OpenAPI from core; regenerate Fetch client; compile and verify no unexplained generated-file diff | Once core/client integration exists |
| System | Build Compose images, bounded startup/health checks, Playwright smoke against actual web/core/Keycloak/database, cleanup | M0 runnable stack; expand with M1 flows |

Activate a check and its required-gate dependency together. Track expected components explicitly; deletion of a manifest from an enabled component is a failure, not permission to skip. Empty services are not green test suites. Java `verify` must actually bind both unit and integration runners; Maven naming conventions alone do not execute integration tests.

For docs-only changes in today's repository, run only repository/architecture checks plus the gate. Do not claim an application build passed. Add deeper checks as components exist rather than fabricate placeholder successful jobs.

### Real dependencies and external calls

Run core database integration tests with Testcontainers on the host runner's Docker, not an in-memory replacement. Add RabbitMQ tests when messaging behavior exists. The system job uses its own isolated Compose stack and fictional users/data; do not also start redundant service containers inside each job.

Standard PR CI uses no Anthropic, Oura, AWS, or personal GitHub credentials. Mock the LLM provider boundary in deterministic tests. When parsing exists, add a separate maintainer-only manually dispatched live-evaluation workflow with an explicit budget, protected credentials, approved dataset, and model/prompt reporting. It executes trusted reviewed code only, never arbitrary fork PR code. Live accuracy/latency targets remain release evidence even though paid evaluations do not run on every PR.

### Runner, dependency, and workflow security

- Set workflow token permissions to `contents: read`; use checkout with credentials persistence disabled. No PR job needs repository write, package publishing, deployments, or OIDC cloud permissions.
- Use `pull_request`, not privileged `pull_request_target`, to build contributor code. Pass PR titles/branch names through environment variables or structured arguments, never interpolate untrusted strings into shell code.
- Pin external actions to verified full commit SHAs with readable version comments. Pin runtime/tool versions and container images; lock npm/uv dependencies. Use a fixed Ubuntu release label and record that hosted image contents still update.
- Cache dependency downloads using lockfile/tool-version keys. Never cache secrets, sessions, private datasets, or built artifacts that could bypass validation. CI must work on a cold cache.
- Public CI artifacts/logs contain synthetic data only. Default artifact retention: 7 days, limited to necessary reports and sanitized failure diagnostics. Disable trace/network dumps that could expose auth credentials, cookies, or tokens. No full environment dumps.
- Proposed timeouts: 5 minutes repository/DSL, 15 minutes per service, 25 minutes system tests. Use bounded health waits and unconditional cleanup. Cancel superseded runs within the same workflow and PR/ref; never cancel unrelated PRs.

### Merge enforcement

Bootstrap: open the first CI PR, verify real passing jobs and one intentional failure/recovery, then add `CI gate` as a required check after GitHub has seen it. Preserve PR-only/squash/no-force-push protection. Require the branch to be up to date; when base changes, bring it into the short-lived feature branch and rerun checks. No second human reviewer is required for a solo maintainer.

The gate checks explicit dependency results rather than assuming `always()` implies success. Test failure, cancellation, expected inapplicability, unexpected skips, docs PRs, and removal of enabled component manifests. Never apply required-check settings before the check exists. A future merge queue would require separate `merge_group` support; none is proposed now.

CI neither auto-merges nor deploys. Signing off author commits remains mandatory; a DCO bot is not introduced. Conventional PR-title validation checks syntax, not the truth of a change description or the semantic version it deserves.

## Alternatives considered

- **Separate required workflows with path filters:** saves work but can leave required checks pending and complicates safe aggregation. Defer until useful.
- **Build everything before code exists:** creates fake reassurance or failures against missing projects. Stage checks alongside real components.
- **Only unit tests:** misses migrations, authorization, contracts, Docker networking, and login integration.
- **Live provider calls on every PR:** variable cost, credentials, nondeterminism, and external outages make ordinary feedback unreliable.
- **Self-hosted laptop runner:** couples contributor execution to a personal machine and its availability. Hosted ephemeral runners suit the current public project.

## Consequences

One stable required check makes branch protection simple while the monorepo grows. Full checks on every PR are initially easier to trust but become slower as services arrive; measure before optimizing. System tests require Docker/browser downloads and careful synthetic auth diagnostics. Pattern-based secret checks are limited detection, not proof of absence; file review remains necessary.

## Open questions before implementation

- Exact pinned checker/Structurizr/action/runtime versions and how validated generated clients are stored; resolve in the CI/scaffold PRs.
- If initial checks reveal preexisting documentation defects, fix links/syntax in drafts or propose explicit errata for accepted ADR history; do not silently weaken checks or rewrite accepted decisions.

## Sources checked 2026-11-05

- [GitHub required-check behavior](https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks): pending skipped workflows and dependency behavior.
- [GitHub secure workflow guidance](https://docs.github.com/en/actions/reference/security/secure-use): minimal permissions, action pinning, and untrusted input.
- [GitHub concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency): cancellation scoped to workflow groups.
- [GitHub Linux container runners](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers): Linux/Docker execution prerequisites.
