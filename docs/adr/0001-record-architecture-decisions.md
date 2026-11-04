# ADR-0001: Record architecture decisions

- Status: Accepted
- Date: 2026-11-04

## Context

Syla is built by one developer working with AI coding agents, and it is meant to show architecture skills. Decisions and the reasons behind them need to stay visible to future maintainers, reviewers and the agents themselves. Without a record, the reasons get lost and the same questions come back.

## Decision

We record every significant architecture decision as an ADR in `docs/adr/`.

- Format: Michael Nygard's template (Context, Decision, Consequences), plus "Alternatives Considered" where useful.
- Files are numbered sequentially: `NNNN-short-title.md`.
- Status is one of: Proposed, Accepted, Deprecated, Superseded by ADR-NNNN.
- Accepted ADRs are not rewritten. A changed decision gets a new ADR that supersedes the old one.
- A decision is significant if it affects structure, quality attributes, dependencies, interfaces or construction techniques, or if it would be expensive to reverse.
- `docs/adr/README.md` holds the index.

## Consequences

- Reviewers can follow why the system looks the way it does.
- AI agents can read the ADRs through steering files and follow the decisions.
- Writing ADRs takes a little time for each decision. That is accepted as part of the work.
