#!/usr/bin/env python3
"""The README must not tell people something the repository contradicts.

It is the first thing anyone reads and the last thing anyone updates. A version in it that the
build does not produce sends every new user to a release that may not exist.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main() -> int:
    readme = (ROOT / "README.md").read_text(encoding="utf-8")
    problems = []

    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    for quoted in set(re.findall(r'version "([0-9]+\.[0-9]+\.[0-9]+)"', readme)):
        if quoted != version:
            problems.append(f"README shows version {quoted}, VERSION says {version}")

    build = (ROOT / "gradle-plugin/build.gradle.kts").read_text(encoding="utf-8")
    plugin_id = re.search(r'id = "([^"]+)"', build).group(1)
    if plugin_id not in readme:
        problems.append(f"README does not mention the plugin id {plugin_id}")

    for _, target in re.findall(r"\[([^\]]*)\]\((?!https?:|#)([^)\s]+)\)", readme):
        if not (ROOT / target.split("#")[0]).exists():
            problems.append(f"README links to {target}, which does not exist")

    if problems:
        print("The README disagrees with the repository:")
        for p in problems:
            print(f"  {p}")
        return 1
    print("  README agrees with the repository")
    return 0


if __name__ == "__main__":
    sys.exit(main())
