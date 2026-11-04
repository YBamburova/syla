# Proposed delivery milestones

Status: Milestone order agreed by the user on 2026-11-04. Detailed scope and acceptance criteria remain a planning draft. No milestones are complete and no release dates are promised. This plan sequences the existing MVP; it does not accept new architecture decisions or remove features from the vision.

Agreed sequence: foundation → personal logging → AI logging → gym classes → progress → social → Oura and MVP completion. The initial [workout examples](workout-examples.md) were also validated as representative.

See the [architecture readiness review](architecture/review-2026-11-04.md) for prerequisites and open questions. M0 precedes M1; M2–M6 build on the personal training model. M4 can move before M3 if progress becomes the higher product priority. Privacy, authorization, accessibility, and relevant tests apply throughout.

## M0 — Reproducible engineering foundation

**Outcome:** a reviewer can start the skeleton and verify it from documented commands.

**Work:** establish the Git/GitHub baseline when authorized; agree web tooling, authentication direction, and CI design before dependent implementation. Add Java core, React shell, PostgreSQL migrations, and the Python service skeleton. Wire Docker Compose with the accepted PostgreSQL/pgvector and RabbitMQ components; no invented embedding or messaging use cases. Publish service contracts and document environment configuration without secrets.

**Acceptance:** clean local startup succeeds; web reaches a core health endpoint; migration and service build checks pass; CI executes actual checks; module boundary verification is enabled for modules present. Python has a health check and bounded build/lint/type/test commands. Document versions, prerequisites, configuration, and current versus planned containers. No claim of full authentication or feature functionality from a health check.

**Documentation:** local deployment view; initial building blocks; CI/tooling decisions; runnable README. Keep this milestone small and split it into separate setup PRs.

## M1 — Personal logging demo

**Demo:** an athlete signs in, logs a strength session and an AMRAP or For Time workout, reloads personal history, then edits and deletes a record.

**Prerequisites:** R1 initial fixtures; R2 authentication and ownership policy; R3 transaction flow; R4 initial data inventory and threat model. Initial formats and visibility must be explicitly agreed in the feature spec.

**Acceptance:** entered values, units, scores, and training date round-trip correctly; invalid scores fail clearly; a second account cannot access or change private records. Repeated submissions do not accidentally duplicate a save. History survives restart. Export includes stored personal data; account deletion removes the slice's data and handles retry/failure. Applicable consent is recorded before processing. Core behavior has meaningful domain and database integration tests; a browser scenario verifies the actual training loop. Manual logging needs neither an LLM key nor an Oura connection.

**Excludes:** AI, gym classes, social feeds, wearable imports, calculated PRs. Mobile-sized screens and keyboard access are included.

**Gate:** demonstrate the loop before extending the domain. This is the first usable demo, not the complete MVP.

## M2 — Reviewed AI logging

**Demo:** text and a permissioned whiteboard photo produce editable sections; the athlete selects performed sections, adds a score, and confirms. A failed parse leads to manual entry.

**Prerequisites:** R5 evaluation/security contract and agent approach ADR; agreed schema shared with the core. Add missing MVP formats, including EMOM and Tabata, and mixed-session support using R1 fixtures.

**Acceptance:** schema-invalid or ambiguous output is handled explicitly; no workout is saved without confirmation; both input paths use the same domain validation. Photo size/type limits, preprocessing, memory-only processing, and discard behavior are verified across the request path. Traces correlate core and Python calls without payloads; token usage, cost, and latency are recorded. External credentials stay server-side. Live evaluations report the vision's ≥90% exact-match target under the defined comparison and p95 ≤3 s text / ≤8 s photo targets under documented conditions. If unmet, report results and retain the gate until tuning or an accepted target revision. Deterministic tests pass without external keys.

**Excludes:** voice, coach agents, production photo storage, automatic programming, speculative insights.

## M3 — Shared gym training

**Demo:** one member publishes a reviewed board; another joins the gym, uses “I was there,” and records an independent result without another LLM request.

**Prerequisites:** R2 audience matrix, R3 attendance consistency, R6 class/ranking rules.

**Acceptance:** public/private joining, approved requests, expiring/revocable invites, and admin management work. Membership and class visibility are enforced in the API. Class corrections preserve existing workout copies; duplicate submissions and concurrent publication follow agreed rules. Class results respect comparability and hidden scores. Private member lists stay restricted in both gym types. Admin departure and empty-gym behavior are tested. Export/deletion includes gym records while retaining shared classes without publisher identity as required by ADR-0010.

**Excludes:** verified owners, gym programming, billing, and global leaderboards. This milestone may contain several feature PRs; it is not one implementation task.

## M4 — Trustworthy progress

**Demo:** repeated comparable training reveals a PR, estimated 1RM, and benchmark history; correcting the source record corrects the chart.

**Prerequisites:** R7 progress rules; R1 benchmark identity; reliable internal event processing where used.

**Acceptance:** deterministic examples prove formula, unit, rounding, scaling, and capped-result behavior. Unsupported comparisons are labeled or excluded. Edits/deletes/replays cannot leave stale PRs or double-count results. Rebuilding derived progress reproduces expected outputs. Personal profile charts obey the agreed audience policy. Export/deletion covers derived data.

## M5 — Social MVP

**Demo:** two athletes follow each other, see permitted logged workouts, like/comment, then change visibility and observe access update.

**Prerequisites:** R2 feed/profile policy, pagination and deletion behavior, and a minimal abuse-handling specification before exposing interactions to real users.

**Acceptance:** follow/unfollow, feed, likes, comments, and pagination work without duplicates. APIs enforce ownership of interactions and all audience rules; private gym information and hidden class scores behave exactly as disclosed by the agreed policy. Visibility changes and deletion remove stale accessible copies. Recovery data is not exposed socially without an explicit approved policy. Export/deletion covers social records.

**Excludes:** challenges, notifications, recommendations, and gym-specific feeds beyond class views.

## M6 — Oura and complete local MVP

**Demo:** an athlete grants consent, connects Oura, sees daily recovery alongside training, then disconnects and deletes their account.

**Prerequisites:** researched Oura access/terms; integration ADR covering OAuth, sync mechanism, dates, token protection, retries, rate limits, and revocation. Close R4 provider and retention questions. Define the first actual cross-service async workflow before introducing consumers; retain accepted RabbitMQ/outbox rules wherever such messaging is used.

**Acceptance:** import is idempotent; time zones, missing/stale data, expired tokens, and provider outages are handled; consent withdrawal stops processing; late jobs do not restore deleted data. Manual logging remains available. API-level privacy checks, module verification, full builds, parser evaluation, and an end-to-end demo pass. Measure typical logging against the <30 s goal with a documented start/end and representative tasks. Complete threat model, quality scenarios, cost notes, and runbook. Verify repository documentation against a clean `docker compose up` run, distinguishing offline demo fixtures from live integrations.

**Release gate:** all MVP capabilities in vision §5 and F1–F8 are demonstrated; unresolved launch blockers are listed. Week-4 retention can only be assessed after real users exist and is not claimed by a local demo. Tagging v1.0.0 requires user authorization and the agreed release checks.

## After the local MVP

AWS hosting is a separate milestone with a deployment ADR, operational costs, secrets, backups, recovery, monitoring, and provider/legal checks before real-user launch. Native apps, other wearables, coaching, MCP, payments, and owner verification remain later-phase scope. Do not make them prerequisites for the first demo.

## Working agreement for every milestone

- Convert the next slice into `docs/specs/<feature>/requirements.md`, `design.md`, and `tasks.md`; give tasks observable outcomes and relevant review IDs.
- Resolve only decisions needed by that slice. The user accepts ADRs and scope changes; routine implementation proceeds within those choices.
- Use small coherent PRs with verification evidence and synchronized documentation. Commit/publish only when authorized.
- End each milestone with a user-visible demonstration and update this plan from what was learned. Record measured results and limitations rather than marking a milestone complete from code generation alone.
