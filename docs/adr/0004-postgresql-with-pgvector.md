# ADR-0004: PostgreSQL with pgvector as the single database

- Status: Accepted
- Date: 2026-11-04

## Context

Syla stores relational data (users, follows, comments), semi-structured workouts (WOD formats vary widely), daily wearable summaries, and embeddings (for example, matching "T2B" or "toes 2 bar" to the movement *toes-to-bar*). A solo developer should run as few database engines as possible.

## Decision

Use PostgreSQL as the only database engine.

- Structured tables for stable concepts; JSONB for format-specific workout details.
- The pgvector extension for embeddings.
- One schema per core module (see ADR-0002).
- Schema changes through Flyway migrations, versioned with the code.
- Oura data is mostly daily summaries, so plain tables are enough. TimescaleDB is reconsidered only if high-frequency data (such as intraday heart rate) is added.

## Alternatives Considered

- **Dedicated vector database (Qdrant, Pinecone).** Rejected: an extra system to run, and the data volume is small.
- **MongoDB for workouts.** Rejected: JSONB covers the flexibility, and relational integrity is needed for social and progress data.
- **TimescaleDB from day one.** Deferred: no high-frequency data in the MVP.

## Consequences

- One engine to run, back up and secure. Amazon RDS for PostgreSQL supports pgvector, which fits the AWS target.
- Vector search performance is enough for a movement library and per-athlete data. Revisit if the data grows by orders of magnitude.
