#!/usr/bin/env bash
set -euo pipefail
psql --username "$POSTGRES_USER" --dbname postgres --set=ON_ERROR_STOP=1 \
  --set=app_password="$SYLA_DB_PASSWORD" --set=keycloak_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
CREATE USER syla PASSWORD :'app_password';
CREATE DATABASE syla OWNER syla;
REVOKE CONNECT ON DATABASE syla FROM PUBLIC;
GRANT CONNECT ON DATABASE syla TO syla;
CREATE USER keycloak PASSWORD :'keycloak_password';
CREATE DATABASE keycloak OWNER keycloak;
REVOKE CONNECT ON DATABASE keycloak FROM PUBLIC;
GRANT CONNECT ON DATABASE keycloak TO keycloak;
SQL
