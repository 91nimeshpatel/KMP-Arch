#!/usr/bin/env python3
"""Every file a bundled skill points at must exist in a project that installed the bundle.

A skill that says "read docs/ADDING_A_FEATURE.md" is useless in a project that does not have it,
and the failure lands on whoever follows the instruction rather than on whoever wrote it.

Note on `@`: inside backticks it is literal text, not an import — Claude Code skips code spans when
it parses imports. So the paths here are checked as paths, which is what they are.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "arch"

# Paths a project writes for itself. A skill names these precisely because they are not shared.
PROJECT_OWNED = {
    "docs/ARCHITECTURE.md",
    "docs/LIBRARIES.md",
    "docs/CONTRIBUTING.md",
}


def main() -> int:
    broken = []
    checked = 0
    for markdown in (ROOT / ".claude/skills").rglob("SKILL.md"):
        text = markdown.read_text(encoding="utf-8")
        for ref in re.findall(r"`(docs/[A-Za-z0-9._/-]+\.md)`", text):
            if ref in PROJECT_OWNED or "<" in ref:
                continue
            checked += 1
            if not (ROOT / ref).is_file():
                broken.append(f"{markdown.parent.name} points at {ref}")

    if broken:
        print("These skills point at files the bundle does not ship:")
        for b in broken:
            print(f"  {b}")
        print("\nAdd the file to the sync list, or treat it as project-owned.")
        return 1
    print(f"  {checked} shared documents referenced, all present")
    return 0


if __name__ == "__main__":
    sys.exit(main())
