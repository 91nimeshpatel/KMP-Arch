#!/usr/bin/env python3
"""SessionStart: notice a project that has not adopted the shared standards, and say so.

Without this, adopting them depends on somebody remembering that a skill exists. The plugin is
already loaded in every session on the machine, so it may as well look at the project it has landed
in and report what is missing.

It deliberately writes nothing. Files appearing in someone's repository because they opened an
editor is a bad surprise, and the difference between a helpful default and an unwelcome one is
whether they were asked. It reports; `kmp:adopt-standards` is what acts.
"""
import json
import sys
from pathlib import Path

# The files that say a project has adopted the standards, and what each one gives it.
EXPECTED = {
    "CLAUDE.md": "the contract every session reads",
    ".claude/rules": "the rules that load when a matching file is opened",
    "architecture-tests": "the executable architecture rules",
    ".github/workflows/ci.yml": "the build gate",
}


def main() -> int:
    payload = json.load(sys.stdin)
    root = Path(payload.get("cwd", "."))

    # Only speak up in a Gradle project. Anywhere else these files would be noise, and a plugin
    # that comments on every repository it lands in gets turned off.
    if not (root / "settings.gradle.kts").is_file() and not (root / "settings.gradle").is_file():
        return 0

    missing = [f"{path} — {why}" for path, why in EXPECTED.items() if not (root / path).exists()]
    if not missing:
        return 0

    if len(missing) == len(EXPECTED):
        message = (
            "This Gradle project has not adopted the shared KMP standards: none of "
            + ", ".join(EXPECTED)
            + " are present. If the user wants them, the `kmp:adopt-standards` skill installs them "
            "and lists what it will change before writing anything. Mention it once; do not run it "
            "unasked, and do not raise it again if they decline."
        )
    else:
        message = (
            "This project has adopted the shared KMP standards, but some are missing:\n  - "
            + "\n  - ".join(missing)
            + "\nThey may have been deleted deliberately. Ask before restoring them with "
            "`kmp:adopt-standards`."
        )

    json.dump(
        {"hookSpecificOutput": {"hookEventName": "SessionStart", "additionalContext": message}},
        sys.stdout,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
