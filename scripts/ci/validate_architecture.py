"""Download, verify, and execute the pinned official Structurizr validator."""

import argparse
import hashlib
import shutil
import subprocess
import tempfile
import urllib.request
from pathlib import Path

VERSION = "2026.09.19"
SHA256 = "ac1a2806e246364b88261a542a36ca7f3006ef48e5de68830afa8672f58690f1"
URL = f"https://download.structurizr.com/structurizr-{VERSION}.war"


def validate(workspace: Path, war: Path) -> None:
    """Check the executable checksum before invoking it on the workspace."""
    with war.open("rb") as stream:
        digest = hashlib.sha256()
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != SHA256:
        raise ValueError("Structurizr checksum mismatch; executable will not run")
    subprocess.run(["java", "-jar", str(war.resolve()), "validate", "-workspace",
                    str(workspace.resolve())], check=True, timeout=180)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--war", type=Path, help="Use an existing download, still checksum-verified")
    parser.add_argument("--workspace", type=Path,
                        default=Path(__file__).resolve().parents[2] / "docs/c4/workspace.dsl")
    args = parser.parse_args()
    if args.war:
        validate(args.workspace, args.war)
    else:
        with tempfile.TemporaryDirectory(prefix="syla-structurizr-") as temporary:
            war = Path(temporary) / "structurizr.war"
            with urllib.request.urlopen(URL, timeout=60) as response, war.open("wb") as output:
                shutil.copyfileobj(response, output)
            validate(args.workspace, war)
    print(f"Architecture valid (Structurizr {VERSION})")


if __name__ == "__main__":
    main()
