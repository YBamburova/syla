# ADR-0003: Polyglot service split

- Status: Accepted
- Date: 2026-11-04

## Context

Syla has three kinds of work with different needs: transactional domain logic, AI agents with evaluation tooling, and exposing data to AI clients over MCP. The project also has an explicit goal of showing skills across Java, Python and TypeScript. Using several languages only for show would be poor architecture, so each language needs a technical reason.

## Decision

| Container | Technology | Reason |
|---|---|---|
| Core API | Java 21, Spring Boot | Complex domain, transactions, strong typing; author's main expertise |
| Agent Service | Python, FastAPI | Most mature ecosystem for agent frameworks, LLM SDKs and evals |
| MCP Server (later phase) | TypeScript, Node.js | Mature MCP SDK; thin layer over the core API |
| Web App | React, TypeScript | Standard choice for a responsive web UI |

Boundary rules:

- The core API is the only system of record. Only it writes domain data.
- The agent service holds no domain data and has no access to domain tables. It receives input, returns proposals or publishes results, and the core API decides what is saved.
- The MCP server reads data only through the core API, with the athlete's permissions.
- Contracts are OpenAPI specs for HTTP and documented message schemas for events.
- The web app calls the core API directly. A separate backend-for-frontend is not added until a real need appears.
- The choice of Python agent framework gets its own ADR.

## Alternatives Considered

- **All Java, with Spring AI for agents.** Viable and simpler to operate. Rejected because the Python ecosystem for agents and evals is ahead, and because learning Python is a stated project goal.
- **All TypeScript or all Python.** Rejected: loses the strengths of Java and Spring for the core domain.

## Consequences

- Three build toolchains and three sets of conventions to maintain.
- Cross-service tracing with OpenTelemetry is required from the first agent.
- Clear trust boundaries: AI components can never change domain data directly.
