#!/usr/bin/env bash
# Copyright (c) 2026 mrlonewolfer. MIT Licence. https://github.com/mrlonewolfer/KMP-Arch
# Proves the scaffolder still generates a feature that passes the gate.
#
# The generator writes Kotlin by hand, so it drifts the moment the codebase moves: a renamed
# package, a deprecated Compose API, a new rule. Its first version was wrong three times over
# exactly that, and each mistake was already answered in feature/welcome — the generator just was
# not reading it.
#
# So this scaffolds a throwaway feature, runs the full gate against it, and removes it. If the
# generator has rotted, this fails instead of the next person finding out.
set -euo pipefail
cd "$(dirname "$0")/.."

NAME="${1:-scaffoldcheck}"
trap 'cleanup' EXIT

cleanup() {
  rm -rf "feature/$NAME" "tools/e2e/pages/$NAME.py" "tools/e2e/tests/test_$NAME.py"
  git checkout -q -- settings.gradle.kts composeApp/build.gradle.kts \
    androidApp/build.gradle.kts docs/ARCHITECTURE.md 2>/dev/null || true
}

if [[ -n "$(git status --porcelain settings.gradle.kts composeApp/build.gradle.kts androidApp/build.gradle.kts docs/ARCHITECTURE.md)" ]]; then
  echo "Uncommitted changes in the files this test rewrites. Commit or stash them first." >&2
  exit 1
fi

echo "==> scaffolding feature/$NAME"
python3 tools/new-feature.py --name "$NAME" > /dev/null

echo "==> running the gate against it"
./gradlew qualityCheck allTests --quiet

echo "==> the scaffolder produces a feature that passes all rules"
