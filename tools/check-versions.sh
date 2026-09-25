#!/usr/bin/env bash
# The plugin version now lives in three places, so it gets a check.
#
#   kmp/.claude-plugin/plugin.json       what Claude Code reads to detect an update
#   kmp/package.json                     what npm publishes
#   kmp/.claude-plugin/marketplace.json  the range the npm source resolves
#
# A mismatch is silent and expensive: npm serves one version while Claude Code believes it has
# another, and `plugin update` reports "already at the latest version" while serving something else.
# That exact failure has already happened once here.
set -euo pipefail
cd "$(dirname "$0")/.."

PLUGIN=$(python3 -c "import json;print(json.load(open('kmp/.claude-plugin/plugin.json'))['version'])")
PACKAGE=$(python3 -c "import json;print(json.load(open('kmp/package.json'))['version'])")
RANGE=$(python3 -c "
import json
m = json.load(open('kmp/.claude-plugin/marketplace.json'))
src = m['plugins'][0]['source']
print(src['version'] if isinstance(src, dict) else '')
")

echo "  plugin.json       $PLUGIN"
echo "  package.json      $PACKAGE"
echo "  marketplace range $RANGE"

fail=0
[[ "$PLUGIN" == "$PACKAGE" ]] || { echo "  MISMATCH: plugin.json and package.json disagree"; fail=1; }

# The range must actually admit the version being published, or npm resolves to something older.
python3 - "$RANGE" "$PLUGIN" <<'PY' || fail=1
import re, sys
rng, version = sys.argv[1], sys.argv[2]
m = re.fullmatch(r"\^(\d+)\.(\d+)\.(\d+)", rng)
if not m:
    print(f"  cannot check range {rng!r} — only ^x.y.z is understood")
    sys.exit(0)
lo = tuple(int(p) for p in m.groups())
cur = tuple(int(p) for p in version.split("."))
# ^0.y.z admits only 0.y.*; ^x.y.z with x>0 admits x.*
ok = cur >= lo and (cur[0] == lo[0]) and (lo[0] > 0 or cur[1] == lo[1])
if not ok:
    print(f"  MISMATCH: range {rng} does not admit {version}")
    sys.exit(1)
PY

[[ $fail -eq 0 ]] || { echo; echo "Fix them, then publish."; exit 1; }
echo "  versions agree"
