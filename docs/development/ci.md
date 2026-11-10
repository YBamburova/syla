# CI rollout

Status: [ADR-0014](../adr/0014-staged-github-actions-ci.md) accepted on 2026-11-05. The repository/architecture workflow passes locally and on [PR #1](https://github.com/YBamburova/syla/pull/1). The [first GitHub run](https://github.com/YBamburova/syla/actions/runs/42029878021) passed Repository, Architecture, and CI gate for commit `fc011b6`. PR #1 was squash-merged as `de04b51`; its [main run](https://github.com/YBamburova/syla/actions/runs/42030385907) passed. A superseded PR run also showed CI gate rejecting cancelled dependencies. Required-check enforcement and hosted broken-link/DSL failure-recovery remain unverified.

CI means continuous integration: every PR gets automated checks before merging. A red result points to a failing check; fix the change and push another commit to the same feature branch. A green result permits review/merge but does not prove the product is complete.

## First CI change

The merged first CI change added: `.github/workflows/ci.yml`, standard-library Python repository/gate checks with regression tests, and real Structurizr validation. The current Java slice adds a real `Core` job running `./mvnw clean verify` on Java 21, plus a `core` gate dependency. It runs on every PR/main push. No path skips or missing-manifest skips. The repository checker always expects the core POM, wrapper script, and wrapper properties; agent/web stay guarded until their own jobs exist.

Core checks now generate interfaces/DTOs from YAML, run domain and Modulith tests, and start real PostgreSQL/Keycloak plus the packaged JAR for integration checks. Flyway migrations and owner/retry/revision/privacy flows are included. The integration suite is under final review; see the personal-logging checkpoint for current status. The core CI job is implemented but not yet run on GitHub. Local core instructions: [build and startup](../../services/core-api/README.md).

Local commands, from the repository root:

```bash
python3 scripts/ci/check_repository.py
python3 -m unittest discover -s scripts/ci -p 'test_*.py'
python3 scripts/ci/validate_architecture.py
```

Python 3.9+ and Java 21+ are sufficient locally; CI pins Python 3.13.9, Temurin 21.0.13+9, and Ubuntu 24.04. The validator downloads the official 210 MB Structurizr 2026.09.19 WAR into temporary storage, verifies the recorded SHA-256, runs `validate`, and deletes its temporary download. To reuse a local download, pass `--war /absolute/path/to/structurizr.war`; the checksum is still verified. The checksum was calculated from the versioned official HTTPS download; it is not a vendor signature.

The supported [Structurizr commands](https://docs.structurizr.com/commands) replace the retired standalone CLI. Its free `validate` command parses the DSL; this check does not render or visually inspect the diagrams. The existing architecture-viewing instructions are independent of CI.

Repository scanning covers tracked and nonignored new files, excluding `.git` and ignored IDE/OS files. It checks ordinary inline and full/collapsed reference Markdown links, repository containment, ATX heading anchors (including duplicates), and explicit HTML anchor IDs. It does not fetch external links, parse every Markdown extension, validate shortcut reference syntax, or inspect binary contents for secrets. Known credential patterns are limited detection; sensitive matches are never echoed. Extend parser fixtures if new Markdown syntax is adopted.

The PR title comes from GitHub's event JSON, not shell interpolation. `CI gate` requires exactly the expected dependency names and successful results; its own regression tests exercise failed/cancelled/skipped/missing/unexpected dependencies. Use fake events locally with `--event /path/to/event.json` to check PR title behavior.

Local review on 2026-11-05: repository checks passed and all 10 checker tests passed using Python 3.9.6. The checksum-verified Structurizr 2026.09.19 validator passed the real workspace using Java 24.0.2 and returned exit 1 for a temporary DSL with an invalid relationship reference. Separate command-line checks confirmed gate rejection of failed/cancelled/skipped dependencies, broken-link failure and correction, and PR-event title acceptance/rejection. The first published run also verified the pinned Ubuntu 24.04/Python 3.13.9/Temurin 21 environment and successful GitHub job aggregation.

1. Completed locally: accepted ADR, reusable repository checker, pinned Structurizr validation, and checker regression tests.
2. Completed locally: existing documentation and DSL pass; accepted historical ADRs were not rewritten.
3. Completed locally: `.github/workflows/ci.yml` includes repository, architecture, and aggregate `CI gate` jobs; the current slice adds Core. No missing-service placeholder jobs.
4. Completed: signed-off commit, published PR #1, and successful GitHub checks. Still pending: demonstrate a failing broken link or invalid DSL and its correction on GitHub; verify the gate fails with its dependency. Local failure/correction evidence is recorded above.
5. After a successful run, require `CI gate` on main and retain solo-friendly PR/squash rules. Merge only with authorization. PR #1 main execution passed; verify each later merged change too.

## Expand during M0/M1

Add core, Python, web, contract, and system jobs in the changes introducing those components. Include their results in the gate and update local commands in `tech.md`. Verify failed builds, cancelled checks, and deleted manifests cannot produce a green gate. Use real PostgreSQL/Keycloak in the relevant integration flows; no cloud or wearable credentials.

M0 system evidence: clean Compose startup, web/core communication, generated-client compatibility, and a real login smoke. M1 evidence extends this to the personal logging requirements, including unauthorized access and account lifecycle.

## Out of scope

Automatic deployments, publishing container images, release automation, paid-model evaluations on every PR, self-hosted runners, and automatic merges. They require later scoped work. Version updates to pinned tools remain normal reviewed changes.
