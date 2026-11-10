# Syla Architecture Documentation

This documentation follows the [arc42](https://arc42.org) template. Diagrams follow the [C4 model](https://c4model.com) and are kept as code in [`../c4/workspace.dsl`](../c4/workspace.dsl). Decisions are recorded as ADRs in [`../adr`](../adr/README.md).

Sections are written as the system grows, not all up front.

| # | Section | Status |
|---|---|---|
| 1 | [Introduction and Goals](01-introduction-and-goals.md) | Draft |
| 2 | [Architecture Constraints](02-architecture-constraints.md) | Draft |
| 3 | [Context and Scope](03-context-and-scope.md) | Draft |
| 4 | [Solution Strategy](04-solution-strategy.md) | Draft |
| 5 | [Building Block View](05-building-block-view.md) | Initial core slice |
| 6 | Runtime View | Planned |
| 7 | [Deployment View](07-deployment-view.md) | Core/DB/auth; Compose run unverified |
| 8 | Cross-cutting Concepts | Planned |
| 9 | Architecture Decisions | See [ADR index](../adr/README.md) |
| 10 | Quality Requirements | Planned |
| 11 | Risks and Technical Debt | Planned |
| 12 | Glossary | Planned |

Related: [Product Vision](../vision.md)

Portfolio work: [System design work plan](design-work-plan.md), including milestone evolution, the first M1 runtime exercise, and verification evidence.

Planning: [Architecture readiness review](review-2026-11-04.md) and [proposed milestones](../milestones.md). These are review recommendations, not accepted ADRs.

The C4 workspace describes the target system. The core health service now exists; relationships to future consumers do not imply working runtime flows.

## Viewing the C4 diagrams

The diagrams render with [Structurizr Lite](https://docs.structurizr.com/lite) from the repository root:

```bash
docker run -it --rm -p 8080:8080 -v "$(pwd)/docs/c4:/usr/local/structurizr" structurizr/lite
```

Then open http://localhost:8080.
