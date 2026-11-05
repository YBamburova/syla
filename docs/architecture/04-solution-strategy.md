# 4. Solution Strategy

## 4.1 Key Decisions

| Topic | Decision | ADR |
|---|---|---|
| Decision process | Significant decisions recorded as ADRs | [0001](../adr/0001-record-architecture-decisions.md) |
| Core structure | Modular monolith in Java 21 / Spring Boot / Spring Modulith, built with Maven | [0002](../adr/0002-modular-monolith-core.md) |
| Service split | Java core API, Python agent service, Node MCP server (later), React web app | [0003](../adr/0003-polyglot-service-split.md) |
| Persistence | PostgreSQL with pgvector, one schema per module, Flyway migrations | [0004](../adr/0004-postgresql-with-pgvector.md) |
| LLM access | Anthropic API called directly, behind a provider interface in the agent service | [0005](../adr/0005-llm-provider-abstraction.md) |
| Messaging | RabbitMQ between services; Spring Modulith events inside the core | [0006](../adr/0006-rabbitmq-for-async-messaging.md) |
| Licensing | Apache 2.0 | [0007](../adr/0007-apache-2-license.md) |
| Runtime | Docker Compose locally, AWS as the target cloud | [0008](../adr/0008-docker-compose-local-aws-target.md) |
| AI logging input | Whiteboard photo and free text in the MVP; photos processed and discarded; voice later | [0009](../adr/0009-whiteboard-photo-parsing.md) |
| Git workflow | Trunk-based, squash merge, Conventional Commits, one SemVer version for the platform, hosted on GitHub | [0011](../adr/0011-git-workflow-and-versioning.md) |
| Gym classes | Public or private gyms; a parsed board can be published as a gym class; members log it with "I was there"; scores visible by default with a hide option | [0010](../adr/0010-gym-classes-and-co-attendance.md) |

## 4.2 How the Quality Goals Are Met

| Quality goal | Approach |
|---|---|
| Usability (logging speed) | AI parsing of whiteboard photos and free text through one pipeline with a fast multimodal model; images are resized and stripped of metadata in the browser; parsing is a synchronous call with a strict timeout; the manual form always works as a fallback ([ADR-0009](../adr/0009-whiteboard-photo-parsing.md)) |
| Data protection | Health data in its own module and schema; consent checked before import; only the minimum data is sent to the LLM; export and deletion built into every module from the start |
| Modifiability | Spring Modulith enforces module boundaries in tests; wearables and LLMs sit behind provider interfaces; modules talk through events, so they can be extracted later |
| Resilience | Oura sync and AI insights run asynchronously over RabbitMQ with retries; timeouts and fallbacks on every external call; logging never depends on an external service |
| AI trustworthiness | Agents only propose; the core API is the system of record and saves only what a human confirmed; eval suite in CI; OpenTelemetry tracing across services; cost tracked per request |

## 4.3 Open Strategy Topics

CI decision: [ADR-0014](../adr/0014-staged-github-actions-ci.md) defines staged GitHub Actions checks and one required merge gate. It is accepted; the first workflow is implemented locally and [rollout](../development/ci.md) awaits an authorized published PR run.

Accepted on 2026-11-04: [ADR-0012: Keycloak OIDC and core-managed sessions](../adr/0012-oidc-login-and-server-sessions.md) and [ADR-0013: web tooling](../adr/0013-web-application-tooling.md). The C4 target includes identity infrastructure; no services are implemented yet. Exact application versions and the documented lifecycle details remain implementation prerequisites.

These need their own ADRs before the related code is written:

- Python agent framework (for example LangGraph or PydanticAI)
- Oura sync model: webhooks, scheduled polling, or both
- AWS deployment architecture and region (an EU region is expected because of GDPR)
