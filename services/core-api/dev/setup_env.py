"""Create local-only Compose secrets without overwriting an existing .env."""

import os
import secrets
from pathlib import Path


def main() -> None:
    path = Path(__file__).resolve().parents[3] / ".env"
    names = ("PG_ADMIN_PASSWORD", "DB_PASSWORD", "KEYCLOAK_DB_PASSWORD",
             "KEYCLOAK_ADMIN_PASSWORD", "KEYCLOAK_SERVICE_SECRET")
    content = "".join(f"{name}={secrets.token_urlsafe(32)}\n" for name in names)
    descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(descriptor, "w") as output:
        output.write(content)
    print("Created .env with local-only secrets. It is ignored by Git.")


if __name__ == "__main__":
    main()
