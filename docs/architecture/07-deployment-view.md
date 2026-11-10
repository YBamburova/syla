# 7. Deployment view

The local [Compose setup](../../docker-compose.yml) targets core, PostgreSQL/pgvector, and Keycloak. Core and provider use separate databases/users; only core can access domain schemas. Host ports bind loopback. Images are pinned by digest. Secrets come from a local ignored `.env`, generated without printing them by the setup helper.

Keycloak uses `http://keycloak.localhost:8081` consistently for browser and core, with a Docker network alias. Its development realm has no human users/passwords in Git; provision fictional local users through its admin console. Core uses server sessions, PKCE, CSRF, and per-service actor checks. The development HTTP/start-dev settings are not a hosted deployment design.

The Docker build stage needs pinned unzip for the checksum-pinned Maven ZIP and runs unit/module tests via `package`. Host `verify` runs real PostgreSQL/Keycloak tests. Full Compose startup and hosted CI remain pending. See [setup/build notes](../../services/core-api/README.md) and the [current test checkpoint](../specs/personal-logging/tasks.md).
