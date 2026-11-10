"""Fail closed unless every expected CI dependency actually succeeded."""

import json
import os
import sys

EXPECTED_JOBS = {"repository", "architecture", "core"}


def gate_passes(needs: dict) -> bool:
    """Reject missing, unexpected, skipped, cancelled, or failed checks."""
    return set(needs) == EXPECTED_JOBS and all(
        item.get("result") == "success" for item in needs.values()
    )


if __name__ == "__main__":
    needs = json.loads(os.environ["CI_NEEDS"])
    passed = gate_passes(needs)
    print("CI gate:", "passed" if passed else "failed; every required job must succeed")
    sys.exit(0 if passed else 1)
