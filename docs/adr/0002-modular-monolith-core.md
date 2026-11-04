# ADR-0002: Modular monolith for the core API

- Status: Accepted
- Date: 2026-11-04

## Context

The core domain has several areas: identity, workouts, progress (PRs, estimated 1RM, benchmarks), social (follows, feed, comments), wearables (Oura) and, later, coaching. They are related but have different rates of change. The team is one part-time developer, so operational overhead must stay low. The architecture should still allow parts to be extracted into services if a real need appears.

## Decision

The core API is a single Spring Boot application (Java 21, Maven) structured as a modular monolith with Spring Modulith.

- Each domain area is an application module in its own top-level package, with a small public API and internal implementation.
- Modules communicate through public module APIs or application events, never through each other's internals.
- Each module owns its data in its own PostgreSQL schema. No cross-module joins or foreign keys.
- Module boundaries are verified by a Spring Modulith test that runs in CI.

## Alternatives Considered

- **Microservices from the start.** Rejected: distributed transactions, network failures and deployment overhead with no team-scaling benefit for a solo developer.
- **Unstructured layered monolith.** Rejected: boundaries erode quickly and later extraction becomes very expensive.

## Consequences

- One deployable, one build, simple local development and debugging.
- Boundaries are enforced by tests, not just convention.
- Event-based communication gives a clear path to extract a module (for example, wearables) into its own service.
- Risk: a shared database can create hidden coupling. Mitigated by schema-per-module and the no-cross-module-joins rule.
