# Repository Structure

Planned monorepo layout. Currently only documentation and agent instructions exist; service directories and commands below are targets, not implemented features.

```
syla/
├── docs/
│   ├── vision.md            # Product vision and decisions log
│   ├── architecture/        # arc42 sections
│   ├── adr/                 # Architecture Decision Records + index
│   ├── development/         # Supporting agent instructions
│   ├── specs/               # Feature requirements, design and tasks
│   └── c4/workspace.dsl     # C4 diagrams (Structurizr DSL)
├── services/
│   ├── core-api/            # Java / Spring Boot modular monolith (system of record)
│   ├── agent-service/       # Python / FastAPI AI agents
│   │   └── evals/           # Eval datasets and runners for the agents
│   └── mcp-server/          # TypeScript MCP server (later phase)
├── web/                     # React + TypeScript web app
├── infra/                   # Infrastructure as code (AWS, later)
├── AGENTS.md                # Codex entry point and reading guide
├── docker-compose.yml
├── LICENSE, NOTICE, README.md
```

## Core API modules

Base package: `com.syla.core`. Each module is a top-level package under it:

| Module | Owns |
|---|---|
| `identity` | Accounts, profiles, authentication integration |
| `workouts` | Workouts, movements, WOD formats, scores |
| `progress` | PRs, estimated 1RM, benchmark history |
| `social` | Follows, feed, likes, comments |
| `gyms` | Gyms, memberships, invites, join requests, classes, attendance |
| `wearables` | Oura connection, imported recovery data |
| `privacy` | Consent, data export, account deletion orchestration |

Inside a module: the public API (interfaces, events, DTOs) sits in the module's root package. Implementation goes into an `internal` subpackage. Other modules may use only the root package.

Each module owns the PostgreSQL schema with the same name. Flyway migrations live in `src/main/resources/db/migration/<module>/`.

## Where new things go

- New feature → a spec in `docs/specs/<feature-name>/` first.
- New significant decision → a new ADR in `docs/adr/`.
- New core domain area → a new module only with an ADR; otherwise extend an existing module.
- Shared contracts (OpenAPI, event schemas) → published by the owning service, never copied by hand into another service.
