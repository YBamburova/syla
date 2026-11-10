# Tech Stack

Each choice is explained in an ADR in [docs/adr](../adr/README.md). Do not introduce a new language, framework, database or infrastructure component without a new ADR. Items marked **TBD** need an ADR before code that depends on them is written; ask the user instead of choosing silently.

| Area | Technology | ADR |
|---|---|---|
| Core API | Java 21, Spring Boot, Spring Modulith, Maven (with Maven Wrapper) | 0002 |
| Persistence | PostgreSQL + pgvector, Spring Data JPA, Flyway | 0004 |
| Messaging | RabbitMQ (AMQP); Spring Modulith events inside the core | 0006 |
| Agent service | Python, FastAPI; agent framework **TBD** | 0003 |
| LLM | Anthropic API, behind a provider interface in the agent service | 0005, 0009 |
| Web app | React + TypeScript, Vite, React Router, TanStack Query, React Hook Form, Material UI, generated Fetch client, Playwright | 0003, 0013 |
| MCP server (later) | TypeScript, Node.js | 0003 |
| Authentication | Keycloak OIDC, Spring Security OAuth2 Login, core-managed sessions | 0012 |
| Observability | OpenTelemetry across all services | 0003 |
| Local runtime | Docker Compose | 0008 |
| Target cloud | AWS (deployment design **TBD**) | 0008 |

## Default tooling

Use these unless an ADR says otherwise:

- **Java:** JUnit 5, AssertJ, Testcontainers, Spring Modulith test support, springdoc-openapi.
- **Python:** uv for dependencies, Ruff for lint and format, mypy for types, pytest for tests, Pydantic for schemas.
- **TypeScript:** ESLint, Prettier, Vitest, Testing Library, strict TypeScript.

## Dependencies

- Pin exact versions. Prefer well-known, actively maintained libraries.
- Check that a library is not already covered by an existing dependency before adding one.
- Mention every new dependency and why it is needed in the PR description.

## Available repository checks

See [CI instructions](ci.md) for the implemented documentation checker, checker regression tests, and pinned architecture validator. PR #1 and its merged main run passed. The new core job still needs an authorized PR run; required-check enforcement is separate.

## Core build (implemented)

Use Java 21 and `./mvnw clean verify` in `services/core-api`. This generates Java APIs/DTOs from YAML and runs unit/module plus real PostgreSQL/Keycloak integration checks; Docker is required. Startup uses PostgreSQL and Keycloak via Compose; see the core instructions. Versions: Boot 3.5.16, Modulith 1.4.14, Wrapper 3.3.4, Maven 3.10.0, OpenAPI Generator 7.27.0, springdoc 2.10.0, pgvector 0.8.7/PostgreSQL 17, Keycloak 26.8.2; CI uses Temurin 21.0.13+9. See [core instructions](../../services/core-api/README.md) for config and limits.

## Planned application commands (not executable yet)

Fill in as services are created. Every service must build and test with one command.

| Service | Build and test |
|---|---|
| agent-service | `uv run pytest` (in `services/agent-service`) |
| web | `npm run test -- --run` (in `web`) |
| Whole system | Full stack planned; current `docker compose up --build` targets core/PostgreSQL/Keycloak; full startup verification is pending |

Use bounded, non-watch commands for verification. Start a development server only when needed for the task; track its session and stop task-owned processes after verification.
