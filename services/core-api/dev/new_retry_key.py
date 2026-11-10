"""Print a fresh UUIDv7 for one new workout save; keep it for retries."""

import secrets
import time
from uuid import UUID

if __name__ == "__main__":
    value = (int(time.time() * 1000) << 80) | (7 << 76) | (secrets.randbits(12) << 64)
    print(UUID(int=value | (2 << 62) | secrets.randbits(62)))
