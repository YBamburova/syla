# 5. Building block view

The Java core contains technical `health` plus `identity`, `privacy`, and `workouts`. Each owns its public generated OpenAPI contract and internal implementation. Modulith verifies the package graph.

Identity owns OIDC mapping, account state, the actor lock, and provider cleanup. Privacy owns consent and in-memory export queries. Workouts owns private sessions, typed section data, revisions, and retry outcomes. Workouts uses public identity/consent queries; privacy uses identity. Synchronous export/deletion events avoid a reverse privacy-to-workouts dependency. There are no cross-schema joins or foreign keys.

See accepted [ADR-0015](../adr/0015-personal-logging-write-safety.md), [backend design](../specs/personal-logging/design.md), and [test checkpoint](../specs/personal-logging/tasks.md). API code is not evidence of a complete M1 UI or verified production behavior. AI, social, gym, progress, and Oura modules do not exist yet.
