#!/usr/bin/env python3
"""PreToolUse (Bash): a store build happens only through tools/release/release.sh.

An app built from this template once shipped 1.0.0 built by hand from main: no release branch, no
tag, version numbers typed in, and the artifacts kept in a temporary folder that an OS upgrade wiped.
The rule was written down but nothing stopped it. This blocks the commands that make a store binary
(signed bundle or APK, Xcode archive, export or upload); the release script runs them itself, where
this hook does not look, and enforces the rule (.claude/skills/release-a-version).
"""
import json
import re
import sys

# The script's own xcodebuild and gradlew calls run inside it, where this hook never sees them, so a
# command naming the script needs no exemption; exempting any command that mentions it would let
# `tools/release/release.sh --help; xcodebuild archive …` through.
STORE_BUILD = re.compile(
    # No \b before -exportArchive: a word boundary never sits between a space and a hyphen, so
    # `\b-exportArchive` let the export, the step that signs and uploads, straight through.
    r"(xcodebuild\b[^\n]*(\barchive\b|-exportArchive\b)"
    r"|\bgradlew?\b[^\n]*\bbundle\w*Release\b"  # any project path or form: bundleRelease, -p androidApp
    r"|\bgradlew?\b[^\n]*\bassemble\w*Release\b"  # a store build only when signed: see UNSIGNED_CHECK
    r"|altool\b[^\n]*--upload"
    r"|xcrun\s+notarytool)"
)
# An unsigned release APK is how the release hardening is checked on a device (docs/TESTING.md);
# only a signed one (keystore in the environment) is a store build.
UNSIGNED_CHECK = re.compile(r"\bassemble\w*Release\b")


def main() -> int:
    payload = json.load(sys.stdin)
    command = payload.get("tool_input", {}).get("command", "")
    match = STORE_BUILD.search(command)
    if not match:
        return 0
    if UNSIGNED_CHECK.search(match.group(0)) and "KEYSTORE_" not in command:
        return 0
    print(
        "Blocked: store builds (signed bundle, Xcode archive/export/upload) go through "
        "tools/release/release.sh only, from release/X.Y with the vX.Y.Z tag on HEAD. "
        "Follow .claude/skills/release-a-version. A build for testing on a device or simulator "
        "uses the Debug configuration instead.",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
