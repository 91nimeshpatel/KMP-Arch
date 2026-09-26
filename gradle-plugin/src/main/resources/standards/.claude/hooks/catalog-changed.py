#!/usr/bin/env python3
"""PostToolUse: tell the agent, the moment it edits the version catalog, what it now owes.

A rule in a document is read once, at the start of a task, and forgotten by the time it matters.
This fires at the exact moment the debt is incurred — the edit itself — and names the versions that
changed, so the agent does not have to remember the rule or work out what it touched.
"""
import json
import re
import sys
from pathlib import Path

CATALOG = "gradle/libs.versions.toml"
PLAYBOOK = "docs/LIBRARIES.md"


def main() -> int:
    payload = json.load(sys.stdin)
    path = payload.get("tool_input", {}).get("file_path", "")
    if not path.endswith(CATALOG):
        return 0

    root = Path(payload.get("cwd", "."))
    catalog = (root / CATALOG).read_text(encoding="utf-8")
    playbook_file = root / PLAYBOOK
    playbook = playbook_file.read_text(encoding="utf-8") if playbook_file.is_file() else ""

    versions = dict(
        re.findall(r'^([A-Za-z0-9_.-]+)\s*=\s*"([^"]+)"', catalog.split("[versions]")[-1].split("[libraries]")[0], re.M)
    )
    used = set(re.findall(r'version\.ref\s*=\s*"([^"]+)"', catalog))
    undocumented = sorted(f"{a} = {v}" for a, v in versions.items() if a in used and v not in playbook)

    if undocumented:
        message = (
            f"You just edited {CATALOG}. These versions are not in {PLAYBOOK}: "
            + ", ".join(undocumented)
            + f". Update {PLAYBOOK} in this same change — the version, the precautions the new "
            "release affects, and its 'On upgrade' line. LibraryPlaybookTest fails the build "
            "otherwise, and a stale entry misleads whoever reads it next."
        )
    else:
        message = (
            f"You edited {CATALOG} and every version still matches {PLAYBOOK}. If this change adds "
            "or removes a dependency, the entry itself still needs writing or deleting."
        )

    json.dump(
        {"hookSpecificOutput": {"hookEventName": "PostToolUse", "additionalContext": message}},
        sys.stdout,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
