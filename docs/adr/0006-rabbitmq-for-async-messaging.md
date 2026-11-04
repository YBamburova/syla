# ADR-0006: RabbitMQ for asynchronous messaging

- Status: Accepted
- Date: 2026-11-04

## Context

Some work must not block the user: importing Oura data, generating insights after a workout, and later the multi-agent coach assistant. Other work is user-facing and needs an immediate answer, like parsing a WOD the athlete just typed. Messages between the core API and the agent service must not be lost if a service restarts.

## Decision

- **Synchronous HTTP** for user-facing requests that need an answer now (WOD parsing), with strict timeouts.
- **Spring Modulith application events** for communication between modules inside the core API.
- **RabbitMQ** for asynchronous communication between services, for example `WorkoutLogged`, `WearableDataImported` and `InsightGenerated`.
- Events leaving the core API are published reliably using Spring Modulith's event publication registry (a transactional outbox) with externalization to AMQP.
- Consumers are idempotent. Failed messages are retried and then moved to a dead-letter queue.

## Alternatives Considered

- **Kafka.** Rejected for now: more operational work, and there is no need for log replay or high throughput. Revisit if event sourcing or analytics streams are needed.
- **AWS SQS/SNS.** Rejected: ties local development to AWS. On AWS, Amazon MQ can host RabbitMQ.
- **Redis Streams.** Rejected: weaker routing and dead-letter support.

## Consequences

- Clear rule for every interaction: sync when the user is waiting, events otherwise.
- The outbox removes the "saved to the database but the event was lost" failure.
- One more component to run locally, handled by Docker Compose.
