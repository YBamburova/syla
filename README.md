# Syla

> **Syla** (Ukrainian: Сила, "strength"). A social training platform for CrossFit and functional athletes.

Log WODs and lifts in seconds, track real progress (PRs, estimated 1RM, benchmark times), see Oura recovery data next to your training, and follow your community.

**Status:** architecture phase. No runnable code yet.

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
