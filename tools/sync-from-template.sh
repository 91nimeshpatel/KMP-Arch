#!/usr/bin/env bash
# Refresh the bundled project files from KMP-Template.
#
#   tools/sync-from-template.sh              # uses ../KMP Template
#   tools/sync-from-template.sh /path/to/it
#   tools/sync-from-template.sh --check      # fail if anything is stale
#
# The template is where these files are written, reviewed and actually exercised by a build. This
# repository ships copies, and a copy drifts — this one was stale within an hour of being made.
# So the copies are refreshed by a script, and CI runs it with --check.
set -euo pipefail
cd "$(dirname "$0")/.."

CHECK=false
TEMPLATE="../KMP Template"
for arg in "$@"; do
  case "$arg" in
    --check) CHECK=true ;;
    *) TEMPLATE="$arg" ;;
  esac
done

[[ -d "$TEMPLATE" ]] || { echo "Template not found at: $TEMPLATE" >&2; exit 1; }

# The bundle ships into other people's repositories, so the template's own package must not travel
# with it. This is not cosmetic: LayerDependencyTest once matched imports against a hard-coded
# `com.vidmira.kmptemplate`, so in every adopting project `imports.none { … }` ran over an empty
# set and two architecture rules passed while checking nothing.
TEMPLATE_PKG=$(sed -n 's/^template\.applicationId=//p' "$TEMPLATE/gradle.properties" | tr -d '"'"'[:space:]'"'"')
[[ -n "$TEMPLATE_PKG" ]] || {
  echo "Could not read template.applicationId from $TEMPLATE/gradle.properties" >&2; exit 1; }
PLACEHOLDER="com.example.app"

# Rewrites the template's package, in both dotted and path form, everywhere under $1.
deproject() {
  local from_path="${TEMPLATE_PKG//./\/}" to_path="${PLACEHOLDER//./\/}"
  find "$1" -type f -print0 |
    xargs -0 perl -pi -e "s/\Q$TEMPLATE_PKG\E/$PLACEHOLDER/g; s/\Q$from_path\E/$to_path/g"
}

# Everything the Gradle plugin installs into a project.
PATHS=(
  CLAUDE.md
  REVIEW.md
  .claude/rules
  .claude/skills
  .claude/agents
  .claude/hooks
  .claude/settings.json
  docs/ADDING_A_FEATURE.md
  docs/TESTING.md
  docs/intents/TEMPLATE.md
  docs/intents/TEMPLATE-spec.md
  docs/intents/README.md
  .github/workflows
  .github/actions
  .github/rulesets
  .github/CODEOWNERS
  .github/pull_request_template.md
  architecture-tests/build.gradle.kts
  architecture-tests/src
  tools/module-graph.py
  tools/apply-branch-protection.sh
  tools/new-feature.py
  tools/test-new-feature.sh
)

TMPCMP=$(mktemp -d)
trap 'rm -rf "$TMPCMP"' EXIT

stale=()
for path in "${PATHS[@]}"; do
  src="$TEMPLATE/$path"
  dst="arch/$path"
  [[ -e "$src" ]] || { echo "  missing in template: $path" >&2; continue; }

  if $CHECK; then
    if [[ "$path" == "CLAUDE.md" ]]; then
      # The bundled copy gains an ownership header and a pointer to the project-owned file, so it
      # never matches byte for byte. Compare what came from the template: everything between them.
      python3 - "$src" "$dst" <<'CMP' || stale+=("$path")
import re, sys
src, dst = (open(p, encoding="utf-8").read() for p in sys.argv[1:3])
body = re.sub(r"^<!--.*?-->\n\n", "", dst, count=1, flags=re.S)
body = body.split("\n---\n\n## This project")[0].rstrip()
src = src.replace("# KMP Template — Engineering Rules", "# Engineering rules", 1).rstrip()
sys.exit(0 if body == src else 1)
CMP
    else
      # Compare against what a sync would actually write: the template's copy with its own package
      # rewritten out of it.
      rm -rf "$TMPCMP"; mkdir -p "$TMPCMP"
      cp -R "$src" "$TMPCMP/copy"
      deproject "$TMPCMP/copy"
      diff -rq "$TMPCMP/copy" "$dst" >/dev/null 2>&1 || stale+=("$path")
    fi
  else
    mkdir -p "$(dirname "$dst")"
    rm -rf "$dst"
    cp -R "$src" "$dst"
    echo "  synced $path"
  fi
done

if $CHECK; then
  # The Gradle plugin's resources are a copy of arch/; they drift the same way.
  if ! diff -rq --exclude=MANIFEST arch "gradle-plugin/src/main/resources/arch" >/dev/null 2>&1; then
    stale+=("gradle-plugin/src/main/resources/arch")
  fi
  if (( ${#stale[@]} )); then
    printf 'Stale bundled files:\n'; printf '  %s\n' "${stale[@]}"
    echo; echo "Run tools/sync-from-template.sh, then bump the plugin version."
    exit 1
  fi
  if grep -rqI --exclude=MANIFEST -- "$TEMPLATE_PKG" arch gradle-plugin/src/main/resources/arch; then
    echo "The template's own package reached the bundle:" >&2
    grep -rnI --exclude=MANIFEST -- "$TEMPLATE_PKG" arch gradle-plugin/src/main/resources/arch >&2
    exit 1
  fi
  echo "  bundled files match the template, and carry none of its package"
  exit 0
fi

# CLAUDE.md is owned by the plugin and points at a file the project owns. The template's copy has
# neither, so the header and the pointer are re-applied after every sync.
python3 - <<'PY'
from pathlib import Path
p = Path("arch/CLAUDE.md"); s = p.read_text(encoding="utf-8")
header = """<!--
  This file is installed by `./gradlew adoptArch` and is REPLACED on every update.
  Do not edit it. Anything specific to this project goes in .claude/project.md, which the plugin
  never writes and never reads back.
-->

"""
s = s.replace("# KMP Template — Engineering Rules", "# Engineering rules", 1)
if "adoptArch" not in s.split("\n")[1:3][0]:
    s = header + s
if "@.claude/project.md" not in s:
    s = s.rstrip() + "\n\n---\n\n## This project\n\n@.claude/project.md\n"
p.write_text(s, encoding="utf-8")
print("  re-applied the ownership header and the project pointer to CLAUDE.md")
PY

echo "==> removing the template's own package from the bundle"
deproject arch
echo "    $TEMPLATE_PKG -> $PLACEHOLDER"

# The Gradle plugin carries the same files, inside its jar. Copied from arch/ rather than from the
# template again, so the jar and the bundle cannot disagree about what the standards are: there is
# one place they are assembled, and the jar reads from it.
echo "==> mirroring into the Gradle plugin's resources"
RESOURCES="gradle-plugin/src/main/resources/arch"
rm -rf "$RESOURCES"
mkdir -p "$RESOURCES"
cp -R arch/. "$RESOURCES/"
find "$RESOURCES" -type f | sed "s|$RESOURCES/||" | sort > "$RESOURCES/MANIFEST"
# MANIFEST lists itself; adoptArch skips it. Count what actually gets installed.
echo "    $(grep -cv '^MANIFEST$' "$RESOURCES/MANIFEST") files"

echo
echo "Now bump VERSION — a fix that does not change the version reaches nobody."
