# Syla

Syla (Сила, “strength”) is a Strava-like social training network for CrossFit athletes and gym people: log meaningful workouts, track progress, and connect with other athletes.

**Status:** private manual running, swimming, cycling, and general workout logging, with Keycloak sign-in, PostgreSQL persistence, editing/deletion, data export, and account deletion. Automatic athlete profiles, search, follow approvals, blocking, and explicit follower-only activity summaries are implemented. The following feed and CrossFit-specific formats come later.

## Documentation

| Document | Purpose |
|---|---|
| [Product](docs/product.md) | Who it serves, initial scope, and later capabilities |
| [Architecture](docs/architecture.md) | Components, ownership, security, and runtime boundaries |
| [Decisions](docs/decisions.md) | All architecture decisions in one table |
| [Development](docs/development.md) | Coding conventions, verification, Git, and CI |
| [Workouts](docs/workouts.md) | Domain model, approved examples, and open scoring rules |

The [C4 workspace](docs/c4/workspace.dsl) describes the target system. [AGENTS.md](AGENTS.md) guides coding agents. Task planning belongs in issues/PRs; no document set is required for each feature.

## Run locally

Prerequisites: a running Docker engine and Compose. For source development also install Java 21 and Node.js 24.22.2 (`web/.node-version`). Maven is supplied by the wrapper.

Create an ignored `.env` with random local secrets if one does not already exist:

```sh
if [ ! -f .env ]; then
  for key in POSTGRES_PASSWORD DB_PASSWORD KEYCLOAK_DB_PASSWORD KEYCLOAK_ADMIN_PASSWORD SYLA_LIFECYCLE_SECRET; do
    printf '%s=%s\n' "$key" "$(openssl rand -hex 24)" >> .env
  done
  chmod 600 .env
fi
```

Start the stack with `docker compose up --build` (or `docker-compose` for a standalone Compose installation). PostgreSQL and Keycloak initialize automatically; register an athlete from the sign-in page. There are no shared athlete passwords in Git. The Keycloak realm is for **local development**, with email verification/delivery disabled.

- Frontend: http://localhost:5173
- Swagger UI (also linked in the frontend footer): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Keycloak: http://localhost:8081

Use `localhost` consistently: the OIDC callback is registered for this origin. Activities start private; an athlete can explicitly share a limited summary with approved followers. Notes and health metrics remain private. There is no GPS tracking or fabricated device metrics. Data is stored in the PostgreSQL named volume and survives service restarts. Do not delete that volume to reset a build.

For source development, start only the dependencies:

```sh
docker compose up -d postgres keycloak
set -a
source .env
set +a
cd services/core-api
./mvnw spring-boot:run
```

In a second terminal:

```sh
cd web
npm ci
npm run dev
```

The frontend stays in `web/` alongside `services/core-api`. API DTOs generate OpenAPI; the Fetch client is generated from that contract. Test and regeneration commands are in [development](docs/development.md); field behavior and UX research are in [workouts](docs/workouts.md).

[GitHub CI](.github/workflows/ci.yml) uses one job for API/domain/PostgreSQL tests, module verification, contract/client consistency, frontend tests/build, and authenticated desktop/mobile browser flows. Hosted execution of this updated workflow is not yet verified.

## License

[Apache License 2.0](LICENSE). The Syla name is excluded; see [NOTICE](NOTICE).

Yuliana Bamburova
