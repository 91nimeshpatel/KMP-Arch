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

# Everything the plugin installs into a project. Keep in step with kmp/skills/adopt-standards.
PATHS=(
  CLAUDE.md
  REVIEW.md
  .claude/rules
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

stale=()
for path in "${PATHS[@]}"; do
  src="$TEMPLATE/$path"
  dst="kmp/project/$path"
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
      diff -rq "$src" "$dst" >/dev/null 2>&1 || stale+=("$path")
    fi
  else
    mkdir -p "$(dirname "$dst")"
    rm -rf "$dst"
    cp -R "$src" "$dst"
    echo "  synced $path"
  fi
done

if $CHECK; then
  if (( ${#stale[@]} )); then
    printf 'Stale bundled files:\n'; printf '  %s\n' "${stale[@]}"
    echo; echo "Run tools/sync-from-template.sh, then bump the plugin version."
    exit 1
  fi
  echo "  bundled files match the template"
  exit 0
fi

# CLAUDE.md is owned by the plugin and points at a file the project owns. The template's copy has
# neither, so the header and the pointer are re-applied after every sync.
python3 - <<'PY'
from pathlib import Path
p = Path("kmp/project/CLAUDE.md"); s = p.read_text(encoding="utf-8")
header = """<!--
  This file is installed by the `kmp` plugin (kmp:adopt-standards) and is REPLACED on every update.
  Do not edit it. Anything specific to this project goes in .claude/project.md, which the plugin
  never writes and never reads back.
-->

"""
s = s.replace("# KMP Template — Engineering Rules", "# Engineering rules", 1)
if "kmp:adopt-standards" not in s:
    s = header + s
if "@.claude/project.md" not in s:
    s = s.rstrip() + "\n\n---\n\n## This project\n\n@.claude/project.md\n"
p.write_text(s, encoding="utf-8")
print("  re-applied the ownership header and the project pointer to CLAUDE.md")
PY

echo
echo "Now bump kmp/.claude-plugin/plugin.json — a fix that does not change the version reaches nobody."
