# Architecture Rules

These rules come from the accepted ADRs. Follow them in all code. If a task seems to require breaking one, document the conflict and propose a new ADR before implementing the conflicting part. Continue independent work. The user accepts significant architecture changes.

See the [ADR index](../adr/README.md).

## System boundaries (ADR-0003)

- The **core API is the only system of record**. Only it writes domain data.
- The **agent service holds no domain data** and never accesses the database. It returns proposals; the core API saves only what a person confirmed.
- The MCP server (later) reads data only through the core API, with the athlete's permissions.
- Services talk only through OpenAPI contracts (HTTP) or documented event schemas (RabbitMQ).

## Core API modules (ADR-0002, ADR-0004)

- Modules use each other only through their public root package, never `internal`.
- Prefer events for cross-module side effects; use direct calls to public APIs only for queries.
- **Each module owns its schema. No SQL joins, foreign keys or JPA relations across schemas.** Refer to other modules' data by ID.
- The Spring Modulith verification test must pass. Never disable or weaken it.

## Messaging (ADR-0006)

- Sync HTTP only when a user is waiting for the answer (for example WOD parsing). Everything else is asynchronous.
- Events leaving the core API go through the Spring Modulith event publication registry (outbox). Never publish to RabbitMQ directly inside a business transaction.
- Every consumer is idempotent. Failed messages retry, then go to a dead-letter queue.
- Event names are past tense facts: `WorkoutLogged`, `GymClassPublished`.

## AI and LLMs (ADR-0005, ADR-0009)

- Only the agent service calls an LLM, and only through the provider interface.
- Model names come from configuration per task, never hard-coded.
- Never send names, emails or account IDs to the LLM. Send only what the task needs.
- Record token usage, latency and cost for every LLM call.
- **Whiteboard photos are processed and discarded.** Never write image data to disk, database, object storage, logs or traces. Traces may record only size and format.
- Every external call (Anthropic, Oura) has a timeout and a fallback. Manual logging must always work when they are down.
- Changes to prompts or parsing logic must keep the eval suite passing.

## Gyms (ADR-0010)

- Each athlete owns their own workout record. A gym class is only a template.
- Gym visibility, admin role and hidden scores are **authorization rules enforced in the core API**, covered by tests. Never rely on the UI to hide data.
- Member lists are members-only, even for public gyms.

## Privacy (GDPR)

- Health and wearable data requires recorded consent before it is imported or shown.
- Every module that stores personal data must support export and deletion through the `privacy` module.
- No names, emails, tokens or health values in logs. Use correlation IDs; treat account IDs as pseudonymous personal data and minimize their use.

## Configuration and runtime (ADR-0008)

- All environment-specific configuration comes from environment variables. No secrets in the repository.
- No AWS-specific APIs in application code without an interface in front of them.
- Every service runs in Docker Compose.
