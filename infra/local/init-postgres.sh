#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname postgres --set=core_password="$DB_PASSWORD" --set=identity_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
CREATE ROLE syla LOGIN PASSWORD :'core_password';
CREATE DATABASE syla OWNER syla;
REVOKE CONNECT ON DATABASE syla FROM PUBLIC;
GRANT CONNECT ON DATABASE syla TO syla;
CREATE ROLE keycloak LOGIN PASSWORD :'identity_password';
CREATE DATABASE keycloak OWNER keycloak;
REVOKE CONNECT ON DATABASE keycloak FROM PUBLIC;
GRANT CONNECT ON DATABASE keycloak TO keycloak;
SQL
