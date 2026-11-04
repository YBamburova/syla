# Documentation Conventions

Architecture documentation is docs as code, in English, reviewed in PRs like code.

## ADRs (`docs/adr/`)

- Format: Context, Decision, Alternatives Considered, Consequences, and Open Questions while Proposed. See [ADR-0001](../adr/0001-record-architecture-decisions.md).
- File name: `NNNN-short-title.md`, next free number.
- New ADRs start as **Proposed**. Only the user accepts an ADR.
- **Never rewrite an Accepted ADR.** A changed decision gets a new ADR that supersedes it; update the old one's status to "Superseded by ADR-NNNN".
- Every ADR added or changed must be reflected in `docs/adr/README.md` (index) and, if it affects strategy, in `docs/architecture/04-solution-strategy.md`.
- If a decision affects product scope, also update the decisions log in `docs/vision.md`.

## arc42 (`docs/architecture/`)

- One file per section: `NN-section-name.md`. Update the status table in `docs/architecture/README.md`.
- Link to ADRs instead of repeating their reasoning.
- Sections are written when the system reaches them, not ahead of time.

## C4 diagrams (`docs/c4/workspace.dsl`)

- All C4 diagrams are kept in the one Structurizr DSL workspace.
- When a container, external system or main relationship changes, update the DSL in the same PR.
- Elements not in the MVP get the `Later` tag.
- Smaller diagrams inside arc42 sections (sequence, flow) use Mermaid.

## Writing style

- Short sentences, plain words, active voice.
- Tables for comparisons and lists of rules; prose for reasoning.
- No marketing language. State facts and tradeoffs.
