"""Check repository documentation, obvious secret patterns, and PR title syntax.

Uses only the Python standard library. Remote links are intentionally not fetched.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote, urlsplit


def without_fences(text: str) -> str:
    """Remove fenced examples while retaining heading text outside them."""
    return re.sub(r"(?ms)^\s*(`{3,}|~{3,}).*?^\s*\1\s*$", "", text)


def prose(text: str) -> str:
    """Remove fenced and inline code from Markdown link scanning."""
    return re.sub(r"`[^`\n]+`", "", without_fences(text))


def anchors(text: str) -> set[str]:
    """Return GitHub-style IDs for ordinary ATX headings, including duplicates."""
    ids: set[str] = set()
    for heading in re.findall(r"(?m)^#{1,6}\s+(.+?)\s*#*\s*$", without_fences(text)):
        heading = re.sub(r"\[([^]]+)\]\([^)]+\)", r"\1", heading)
        base = re.sub(r"[^\w\- ]", "", heading.lower()).replace(" ", "-")
        candidate = base
        count = 0
        while candidate in ids:
            count += 1
            candidate = f"{base}-{count}"
        ids.add(candidate)
    ids.update(re.findall(r'<(?:a|h[1-6])\b[^>]*(?:id|name)=[\'"]([^\'"]+)', text))
    return ids


def markdown_errors(root: Path, path: Path) -> list[str]:
    """Validate local inline/reference links and Markdown target anchors."""
    text = prose(path.read_text(encoding="utf-8"))
    definitions = dict(re.findall(r"(?m)^\s*\[([^]]+)\]:\s*(\S+)", text))
    definitions = {key.lower(): value for key, value in definitions.items()}
    destinations = re.findall(r"!?\[[^]\n]*\]\(\s*(<[^>]+>|[^\s)]+)(?:\s+[^)]*)?\)", text)
    errors = []
    for label, reference in re.findall(r"\[([^]\n]+)\]\[([^]\n]*)\]", text):
        key = (reference or label).lower()
        if key not in definitions:
            errors.append(f"undefined link reference: {key}")
        else:
            destinations.append(definitions[key])
    for destination in destinations:
        url = urlsplit(destination.strip("<>"))
        if url.scheme or url.netloc:
            continue
        target = (root / unquote(url.path).lstrip("/") if url.path.startswith("/")
                  else path.parent / unquote(url.path)) if url.path else path
        target = target.resolve()
        if not target.is_relative_to(root.resolve()):
            errors.append("local link escapes repository")
        elif not target.exists():
            errors.append(f"missing local link: {destination}")
        elif url.fragment and target.suffix.lower() == ".md":
            if unquote(url.fragment) not in anchors(target.read_text(encoding="utf-8")):
                errors.append(f"missing anchor: {destination}")
    return errors


def secret_patterns() -> list[re.Pattern[str]]:
    """Known credential shapes; not a comprehensive secret scanner."""
    return [re.compile(pattern) for pattern in (
        r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
        r"gh[pousr]_[A-Za-z0-9]{20,}",
        r"github_pat_[A-Za-z0-9_]{30,}",
        r"AKIA[A-Z0-9]{16}",
        r"sk-ant-[A-Za-z0-9_-]{20,}",
    )]


def repository_errors(root: Path, files: list[Path]) -> list[str]:
    """Check selected files without printing potentially sensitive matches."""
    errors = []
    for path in files:
        relative = path.relative_to(root)
        if not path.resolve().is_relative_to(root.resolve()):
            errors.append(f"{relative}: symlink escapes repository")
            continue
        if path.name == ".env" or (path.name.startswith(".env.") and path.name != ".env.example"):
            errors.append(f"{relative}: environment secrets file must not be tracked")
        if not path.is_file():
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if any(pattern.search(content) for pattern in secret_patterns()):
            errors.append(f"{relative}: possible credential detected (value suppressed)")
            # Link diagnostics include destinations, which could contain the credential.
            continue
        if path.suffix.lower() == ".md":
            errors.extend(f"{relative}: {error}" for error in markdown_errors(root, path))
    # Until a service's real job is added, adding it must fail instead of going unchecked.
    for component in ("services/agent-service", "web"):
        if (root / component).exists():
            errors.append(f"{component}: add its real CI job and gate dependency before enabling this component")
    for manifest in ("services/core-api/pom.xml", "services/core-api/mvnw",
                     "services/core-api/.mvn/wrapper/maven-wrapper.properties"):
        if not (root / manifest).is_file():
            errors.append(f"{manifest}: required core build file is missing")
    return errors


def title_valid(title: str) -> bool:
    """Validate the repository's Conventional Commit PR title convention."""
    return len(title) <= 70 and bool(re.fullmatch(
        r"(?:feat|fix|docs|refactor|test|perf|build|ci|chore)(?:\([a-z0-9-]+\))?!?: [a-z0-9](?:.*[^.])?", title
    ))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--event", type=Path, help="GitHub event JSON, to check PR title")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    output = subprocess.check_output(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=root
    )
    files = [root / name.decode() for name in output.split(b"\0") if name]
    errors = repository_errors(root, files)
    if args.event:
        event = json.loads(args.event.read_text())
        if "pull_request" in event and not title_valid(event["pull_request"]["title"]):
            errors.append("PR title must be Conventional Commits format, lowercase start, <=70 characters, no final period")
    for error in errors:
        print(error, file=sys.stderr)
    print(f"Repository checks: {len(files)} files, {len(errors)} errors")
    return int(bool(errors))


if __name__ == "__main__":
    sys.exit(main())
