#!/usr/bin/env python3
"""Stop: refuse to finish a turn while the documents contradict the code.

The rules this checks are also `:architecture-tests` tests, so CI catches them either way. This
hook exists because CI is minutes away and the agent is here now, with the context to fix it in
seconds. It runs no Gradle — pure file reads, a few milliseconds.

It blocks at most once per session for a given set of problems. An agent that cannot fix them
should be able to say so rather than being trapped in a loop.
"""
import hashlib
import json
import re
import sys
from pathlib import Path


def problems(root: Path) -> list[str]:
    found: list[str] = []

    catalog_file = root / "gradle/libs.versions.toml"
    playbook_file = root / "docs/LIBRARIES.md"
    if catalog_file.is_file() and playbook_file.is_file():
        catalog = catalog_file.read_text(encoding="utf-8")
        playbook = playbook_file.read_text(encoding="utf-8")
        versions = dict(
            re.findall(
                r'^([A-Za-z0-9_.-]+)\s*=\s*"([^"]+)"',
                catalog.split("[versions]")[-1].split("[libraries]")[0],
                re.M,
            )
        )
        used = set(re.findall(r'version\.ref\s*=\s*"([^"]+)"', catalog.split("[libraries]")[-1]))
        for alias, version in sorted(versions.items()):
            if alias in used and version not in playbook:
                found.append(f"docs/LIBRARIES.md does not mention {alias} = {version}")

    architecture = root / "docs/ARCHITECTURE.md"
    if architecture.is_file():
        text = architecture.read_text(encoding="utf-8")
        seams = set()
        for kt in (root / "core").rglob("*.kt"):
            if "/build/" in str(kt):
                continue
            seams |= set(
                re.findall(
                    r"^\s*(?:internal\s+)?expect\s+(?:fun|val|var|class|object|interface)\s+"
                    r"(?:[A-Za-z0-9_.]+\.)?([A-Za-z_][A-Za-z0-9_]*)",
                    kt.read_text(encoding="utf-8"),
                    re.M,
                )
            )
        for seam in sorted(seams):
            if seam not in text:
                found.append(f"docs/ARCHITECTURE.md does not mention the platform seam `{seam}`")

    return found


def main() -> int:
    payload = json.load(sys.stdin)
    # The repository this hook belongs to, not the session's current directory: from a subfolder the
    # current directory missed the documents and the code, and the turn ended unchecked.
    root = Path(__file__).resolve().parents[2]
    found = problems(root)
    if not found:
        return 0

    # One block per distinct set of problems, so a genuinely stuck agent can still finish and say so.
    scratch = Path(payload.get("scratchpad_dir") or "/tmp")
    scratch.mkdir(parents=True, exist_ok=True)
    marker = scratch / ("docs-gate-" + hashlib.sha1("\n".join(found).encode()).hexdigest()[:12])
    if marker.exists():
        json.dump(
            {
                "hookSpecificOutput": {
                    "hookEventName": "Stop",
                    "additionalContext": "Documents are still out of step: " + "; ".join(found)
                    + ". Say so explicitly in your reply rather than leaving it unsaid.",
                }
            },
            sys.stdout,
        )
        return 0

    marker.write_text("blocked once", encoding="utf-8")
    print(
        "Not done yet — the documents contradict the code:\n  - "
        + "\n  - ".join(found)
        + "\n\nFix them now, in this change: a document that has drifted is worse than none, because "
        "the next person trusts it. Run `./gradlew :architecture-tests:test` to confirm.",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
