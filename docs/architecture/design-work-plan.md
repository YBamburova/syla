# System design portfolio work plan

Status: proposed documentation work, 2026-11-10. This plan does not accept new architecture decisions or claim implemented behavior. The [milestones](../milestones.md) remain the delivery plan.

## Purpose

Show how product requirements become system boundaries, consistency rules, failure handling, and verification. Use Syla's real constraints rather than an invented large-scale deployment. A reviewer should be able to trace a requirement to a decision, a runtime flow, and evidence or an explicit open question.

The [building block view](05-building-block-view.md) describes the implemented core health slice. The [C4 workspace](../c4/workspace.dsl) describes the target architecture. Keep those statuses distinct as implementation grows.

## Architecture evolution

Document changes at meaningful boundaries rather than drawing a complete new architecture for every milestone. Keep C4 views in the existing workspace; use Mermaid for runtime sequences in arc42 section 6 when those flows are designed. Do not maintain competing diagram sources.

| Stage | Design question | Useful artifact | Evidence required before claiming it works |
|---|---|---|---|
| Current M0 slice | What exists and what can a reviewer run? | Current building blocks and deployment view | Build, module verification, packaged-JAR smoke; record Docker and hosted-CI limitations |
| M1 personal logging | Who can write a workout, and what happens after retries or concurrent changes? | Save/retry and edit/delete sequences; transaction and ownership boundaries | Database race tests, cross-account access tests, browser save/reload flow |
| M2 reviewed AI logging | How does an unreliable external model assist without owning data? | Parse/review/confirm sequence and photo data lifecycle | Invalid-output and outage tests, parser evaluation, memory-only photo handling checks |
| M3–M5 shared training and derived views | How do independent workout copies, progress, and social access remain consistent? | Ownership map, change-event flows, visibility and correction scenarios | Duplicate/replay tests, derived-data rebuilds, authorization tests |
| M6 wearable integration | How do imports respect consent, provider failures, and deletion? | Sync lifecycle, trust boundaries, late-job/deletion sequence | Idempotent imports, revocation and deletion-race tests, outage behavior |
| Later hosted deployment | What operational need justifies deployment complexity? | Proposed deployment ADR, capacity/cost model, recovery runbook | Documented workload assumptions followed by measurements and restore exercises |

Accepted technologies remain governed by the [ADR index](../adr/README.md). New significant choices start as Proposed ADRs. Do not introduce a service, queue consumer, cache, or embedding workflow just to fill a diagram.

## First design exercise: a reliable private workout save

Use [personal logging requirements](../specs/personal-logging/requirements.md) and its [design](../specs/personal-logging/design.md) as the source. The first exercise should resolve behavior needed for M1, while exposing unresolved choices explicitly.

1. Describe the normal save from authenticated browser to committed workout and response. Mark account eligibility, consent, validation, transaction boundaries, and module public APIs.
2. Describe a lost response followed by two simultaneous retries. Define the actor/key/payload binding, database enforcement, replay outcome, retention window, and expired-key behavior.
3. Describe an edit from a stale browser tab. Identify the revision check and the user-visible conflict without losing unsaved input.
4. Describe account deletion racing a save. Specify a mutation barrier, ordering, cleanup retries, and how stale work is rejected through public module contracts. A preliminary account-state check alone is not a barrier.
5. Compare plausible solutions and record their costs. Cross-schema joins, foreign keys, and direct access to another module's internals remain prohibited.

Deliver runtime sequences in arc42 section 6, initial quality scenarios in section 10, and unresolved risks in section 11. Update the personal-logging spec with the resulting design and verification tasks. Add a Proposed ADR only where a significant decision is needed. This work plan does not choose a locking or deletion protocol.

## Observable quality scenarios

These are proposed review scenarios, not verified guarantees or new performance targets.

| Situation | Expected behavior to design | Verification |
|---|---|---|
| Two concurrent creates use the same actor, key, and payload | One workout and a defined consistent replay outcome | PostgreSQL integration test with controlled concurrent requests |
| A previously successful create is retried after workout deletion | No resurrection and no misleading current-record response | Delete/replay integration test |
| Another athlete requests a private workout | No data disclosure; response follows the agreed missing/nonowned policy | API authorization tests |
| An old editor submits after a newer edit | Conflict; the newer record remains intact | Revision race test and browser conflict scenario |
| A save overlaps account deletion | No new personal data survives completed cleanup | Controlled database race and cleanup-retry tests |
| AI or Oura is unavailable | Manual logging remains usable | Dependency-outage scenario once the relevant integration exists |

## Capacity and operational reasoning

Add a small capacity model when a runnable workload exists. Label user counts, sessions per day, payload sizes, and peak factors as assumptions. Derive request rates, data growth, and external-call demand. Separate peak traffic from daily averages and sensitivity ranges from measured results.

Measure the first likely bottleneck before proposing infrastructure changes. Define a load profile and latency/error criteria before running a test. Preserve reproduction commands and environment details. Cloud costs remain estimates until deployment choices and prices are checked; do not invent a monthly bill now.

## Reviewer walkthrough

A short walkthrough should follow one athlete's workout through the system:

1. Establish the current implementation and target scope.
2. Explain the private save and the most important invariant.
3. Trace one retry or deletion race and the chosen trade-off.
4. Show verification evidence, or label the test as planned.
5. Explain how AI, shared classes, and wearables extend that foundation.

Prefer a few legible diagrams with concrete failure paths over a large diagram inventory. Keep measured evidence, assumptions, accepted decisions, proposed choices, and open questions visibly distinct.
