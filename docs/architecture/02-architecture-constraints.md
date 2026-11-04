# 2. Architecture Constraints

## 2.1 Technical Constraints

| ID | Constraint | Background |
|---|---|---|
| TC1 | Core backend in Java 21 (LTS), Spring Boot, built with Maven | Author's core expertise; see [ADR-0002](../adr/0002-modular-monolith-core.md) |
| TC2 | AI agent service in Python | AI ecosystem and learning goal; see [ADR-0003](../adr/0003-polyglot-service-split.md) |
| TC3 | MCP server in TypeScript / Node.js (later phase) | MCP SDK maturity and learning goal; see ADR-0003 |
| TC4 | Phase 1 client is a responsive React + TypeScript web app; no native mobile app | Scope decision in the [vision](../vision.md) |
| TC5 | PostgreSQL as the only database engine | See [ADR-0004](../adr/0004-postgresql-with-pgvector.md) |
| TC6 | RabbitMQ for asynchronous messaging | See [ADR-0006](../adr/0006-rabbitmq-for-async-messaging.md) |
| TC7 | Whole system runs locally with `docker compose up`; AWS is the target cloud | See [ADR-0008](../adr/0008-docker-compose-local-aws-target.md) |
| TC8 | Anthropic API is the LLM provider, called directly | See [ADR-0005](../adr/0005-llm-provider-abstraction.md) |
| TC9 | Oura is integrated through its cloud API v2 with OAuth2 | First wearable, chosen in the vision |

## 2.2 Organizational Constraints

| ID | Constraint | Background |
|---|---|---|
| OC1 | Solo developer, part-time | Favors low operational overhead and few moving parts |
| OC2 | Development is AI-assisted with Codex; repository instructions and feature specs live in the repo | The development workflow is part of the showcase |
| OC3 | Public repository under Apache 2.0; no secrets in the repo | See [ADR-0007](../adr/0007-apache-2-license.md) |
| OC4 | Docs as code: arc42, C4 (Structurizr DSL) and ADRs, in English | Versioned with the code, reviewable in PRs |
| OC5 | Every significant decision is recorded as an ADR | See [ADR-0001](../adr/0001-record-architecture-decisions.md) |

## 2.3 Legal and Compliance Constraints

| ID | Constraint | Background |
|---|---|---|
| LC1 | GDPR: health and wearable data is special-category data (Art. 9), processed only with explicit consent | Users in the EU are expected |
| LC2 | Oura API terms are followed and reviewed before any commercial launch | Third-party data access |
| LC3 | Anthropic usage policies are followed; only the minimum data needed is sent to the LLM | Data minimization |
| LC4 | No medical diagnosis or medical advice | Avoids medical-device regulation; insights stay about training |

## 2.4 Conventions

- Architecture docs: arc42; diagrams: C4; decisions: ADRs in Michael Nygard's format
- Commit messages: [Conventional Commits](https://www.conventionalcommits.org)
- API contracts: OpenAPI, generated from code and published with each service
