# Core backend

Java 21, Boot 3.5.16, Modulith 1.4.14. This stage adds private manual workout APIs to the earlier health foundation. It is not the full M1 product: no React app or browser demo exists yet.

## Start locally

Install Docker/Compose and JDK 21. From the repository root:

```bash
python3 services/core-api/dev/setup_env.py
docker compose up --build
```

The setup script creates an ignored, mode-600 `.env` with random local-only secrets and refuses to overwrite one. Compose starts PostgreSQL/pgvector, Keycloak, and core. Data uses a named volume. `docker compose down` stops it without deleting data. Do not use `down --volumes` for data you want to keep.

Keycloak: `http://keycloak.localhost:8081`. The browser should resolve `*.localhost` to loopback; add `127.0.0.1 keycloak.localhost` to your hosts file if your OS/browser does not. Containers use the same name through a network alias, preserving issuer checks. The imported realm is `syla`. Sign in to the admin console as `local-admin` with `KEYCLOAK_ADMIN_PASSWORD` from your private `.env`. Create a clearly fictional local user and set its password; public registration is off. Core and Keycloak use separate database users/databases. The provider cannot access core tables.

Login: `http://localhost:8080/oauth2/authorization/keycloak`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`. The UI uses the server session and CSRF cookie. Select Account and call `GET /api/v1/csrf` after login if needed, then Privacy to grant training consent. No provider token is put in the browser or Swagger authorization store. Logout is a CSRF-protected `POST /logout` and uses OIDC provider logout.

Health: `curl -fsS http://localhost:8080/api/v1/health`. It reports process health, not DB readiness.

`CORE_PORT` and `DB_PORT` change host ports. Set the core port before importing the realm for the first time; after that, update the realm's exact callback/logout URLs if you change it. The keycloak host/port must stay consistent for browser and core. These HTTP/start-dev settings are local-only.

## API contracts

Edit YAML in `src/main/openapi`. Maven OpenAPI Generator 7.27.0 generates Spring interfaces and record DTOs into `target/generated-sources/openapi`. Implement those interfaces; never edit generated sources or copy DTOs by hand. Each module owns its own contract and generated public types. Springdoc 2.10.0 supplies Swagger UI and the runtime `/v3/api-docs`; the UI reads the authoritative YAML at `/openapi/{health,workouts,identity,privacy}.yaml`.

| Route | Purpose |
|---|---|
| `GET /api/v1/health` | Public process health |
| `GET /api/v1/account`, `GET /api/v1/csrf` | Current account and CSRF bootstrap |
| `GET/POST /api/v1/workouts` | Private history and create |
| `GET/PUT/DELETE /api/v1/workouts/{id}` | Private detail, replace, delete |
| `GET/PUT /api/v1/consents/training` | Consent v1 and withdrawal |
| `GET /api/v1/privacy/export` | JSON file with this actor's stored data |
| `DELETE /api/v1/account` | Block writes, purge local data, delete provider user |

Workout examples are in `src/test/resources/{strength,amrap,for-time}.json`. Keep prescription separate from actual work. Missing sets differ from zero reps; unscored results differ from zero scores. AMRAP normalizes complete extra rounds. Completed/capped For Time scores are separate types. Loads use BigDecimal with explicit kg/lb and up to three decimal places. The initial movement catalog is in the YAML. JSON bodies are limited to 256 KiB; no AI or wearable calls are made.

Create needs a UUIDv7 `Idempotency-Key`. Run `python3 services/core-api/dev/new_retry_key.py` from the repo root and retain that key for retries within 24 hours. A changed payload or an edited/deleted replay outcome returns 409. Edits/deletes need the current quoted `If-Match` revision, for example `"0"`; stale revisions return 409. GET/create/replace return ETag. Missing and nonowned records share 404; a repeated delete is safe and returns 404. History has bounded cursor pagination.

Consent withdrawal blocks training access but keeps export/deletion available. Deletion returns PENDING if provider cleanup fails, with automatic retry; COMPLETED means training/consent/retry data and the provider user are removed. The disclosed minimal identity tombstone remains for 30 days, then is purged. See accepted [ADR-0015](../../docs/adr/0015-personal-logging-write-safety.md). Consent wording is for the local demo, not a public launch policy.

## Build and checks

```bash
cd services/core-api
./mvnw --batch-mode --no-transfer-progress clean verify
```

The checksum-pinned wrapper downloads Maven 3.10.0; no system Maven is needed. First use needs HTTPS, curl/wget, and unzip. Java must be major 21 (CI pins Temurin 21.0.13+9). Boot/Modulith BOMs pin JPA, Hibernate, PostgreSQL JDBC, Flyway, and Testcontainers. Flyway owns migrations; Hibernate validates and never creates/updates the schema.

`verify` runs domain/retry tests, Modulith boundary verification, and packaged-JAR tests with real PostgreSQL and Keycloak containers. It requires a working Docker daemon and uses only ephemeral fictional users/data. The built-image stage runs `package` (unit/module tests); DB/provider integration checks run on the host through `verify`, without Docker-in-Docker. Reports and runtime OpenAPI export are under `target/`.

On this Homebrew/Colima setup, use `colima start --profile syla-dev`, the `docker-compose` standalone command if the CLI plugin is not configured, and these variables before Maven:

```bash
export DOCKER_HOST="unix://$HOME/.colima/syla-dev/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

For a host JAR run, start DB/Keycloak first, load the private `.env`, and pass `DB_URL`, `DB_USER`, `DB_PASSWORD`, `OIDC_ISSUER_URI`, and `KEYCLOAK_SERVICE_SECRET`. The issuer hostname must resolve from that JVM too. Then run `java -jar target/core-api.jar`. Sessions use a 30-minute idle and 12-hour absolute limit; restart signs users out while DB data stays. Use `SESSION_COOKIE_SECURE=true` behind HTTPS.

No AI, social, gym, progress, Oura, paid calls, or deployment work is included. Full Compose startup, frontend behavior, and hosted CI need their own final verification; see the checkpoint in the personal-logging tasks.

Current checkpoint: 11 unit/module tests and 5 of 6 real-provider/DB integration tests pass. Provider deletion returns HTTP 401, leaving account cleanup PENDING; the deletion integration test is intentionally still failing. Resolve it before merge. Full Compose startup and hosted CI remain unverified.
