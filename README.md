# Syla

> **Syla** (Ukrainian: Сила, "strength"). A social training platform for CrossFit and functional athletes.

Log WODs and lifts in seconds, track real progress (PRs, estimated 1RM, benchmark times), see Oura recovery data next to your training, and follow your community.

**Status:** M0 in progress. The Java core has private logging API code, OpenAPI contracts, and DB/auth setup; login/logging checks pass, but provider account cleanup still fails (HTTP 401); this stage is not complete M1.

## Run the backend

```bash
python3 services/core-api/dev/setup_env.py
docker compose up --build
```

This starts core, PostgreSQL/pgvector, and Keycloak. Use fictional local users. Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Login: `/oauth2/authorization/keycloak`. See [setup, API, and checks](services/core-api/README.md) for user setup and limits. OpenAPI YAML generates the Java interfaces/DTOs.

Build with JDK 21 and Docker: `cd services/core-api` then `./mvnw clean verify`. The full M1 UI/demo is still pending.

## Documentation

- [Personal logging specification](docs/specs/personal-logging/requirements.md)
- [Workout domain model](docs/workout-model.md)
- [Agreed workout examples](docs/workout-examples.md)
- [Proposed milestones](docs/milestones.md)
- [Architecture readiness review](docs/architecture/review-2026-11-04.md)
- [Product Vision](docs/vision.md)
- [Architecture (arc42)](docs/architecture/README.md)
- [Architecture Decision Records](docs/adr/README.md)
- [C4 diagrams (Structurizr DSL)](docs/c4/workspace.dsl)

## Development with Codex

Repository checks: `python3 scripts/ci/check_repository.py` and `python3 -m unittest discover -s scripts/ci -p 'test_*.py'`. Architecture validation: `python3 scripts/ci/validate_architecture.py` (requires Java 21+ and downloads a checksum-pinned validator). See [CI instructions](docs/development/ci.md).

Start with [AGENTS.md](AGENTS.md). Supporting instructions live in [docs/development](docs/development/README.md); feature specifications belong in `docs/specs/<feature>/`. No custom skills are required initially.

## Planned Stack

Java 21 / Spring Boot (core API), Python (AI agents), TypeScript / React (web app), PostgreSQL + pgvector, RabbitMQ, Docker Compose, AWS as target cloud. Each choice is justified in an ADR.

## License

[Apache License 2.0](LICENSE). The "Syla" name is not covered by the license (see [NOTICE](NOTICE)).

## Author

Yuliana Bamburova
