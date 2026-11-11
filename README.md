# Syla

Syla (Сила, “strength”) is a Strava-like social training network for CrossFit athletes and gym people: log meaningful workouts, track progress, and connect with other athletes.

**Status:** product and architecture groundwork. No application source/build or Docker Compose configuration is tracked yet.

## Documentation

| Document | Purpose |
|---|---|
| [Product](docs/product.md) | Who it serves, initial scope, and later capabilities |
| [Architecture](docs/architecture.md) | Components, ownership, security, and runtime boundaries |
| [Decisions](docs/decisions.md) | All architecture decisions in one table |
| [Development](docs/development.md) | Coding conventions, verification, Git, and CI |
| [Workouts](docs/workouts.md) | Domain model, approved examples, and open scoring rules |

The [C4 workspace](docs/c4/workspace.dsl) describes the target system. [AGENTS.md](AGENTS.md) guides coding agents. Task planning belongs in issues/PRs; no document set is required for each feature.

The planned stack is Java/Spring core, Python AI assistance, React/TypeScript web, and PostgreSQL. The decision table records the additional planned components and tradeoffs. [GitHub CI](.github/workflows/ci.yml) currently checks patch whitespace and explicitly reports that application tests are not configured. Add real tests with the first application code.

## License

[Apache License 2.0](LICENSE). The Syla name is excluded; see [NOTICE](NOTICE).

Yuliana Bamburova
