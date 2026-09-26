#!/usr/bin/env bash
# Copyright (c) 2026 Nimesh Patel. MIT Licence. https://github.com/91nimeshpatel/KMP-Arch
# Apply this project's branch rules to the GitHub repository.
#
#   tools/apply-branch-protection.sh              # the current repo's origin
#   tools/apply-branch-protection.sh owner/repo
#
# Without this, "main is always releasable, no direct pushes" is a sentence nothing enforces, and
# the twenty checks in :architecture-tests can be skipped by pushing straight to main.
#
# Branch protection is free on **public** repositories, on every plan. It is the combination of
# *private* and Free that GitHub refuses — both the rulesets API and the older branch-protection
# API answer 403 there. This script applies what Free allows either way, and says plainly what is
# missing. See docs/RELEASE_PLAN.md §1.
#
# Needs the `gh` CLI, signed in with admin rights on the repository.
set -euo pipefail
cd "$(dirname "$0")/.."

REPO="${1:-$(gh repo view --json nameWithOwner --jq .nameWithOwner)}"
RULESET=".github/rulesets/main.json"

command -v gh >/dev/null || { echo "gh is required: https://cli.github.com"; exit 1; }
[[ -f "$RULESET" ]] || { echo "missing $RULESET"; exit 1; }

echo "==> $REPO"

# --- What every plan allows -----------------------------------------------------------------
# Squash-only keeps history linear, and auto-delete stops merged branches piling up. Both are
# assumed by the branch model, and neither needs a paid plan.
echo "==> merge settings: squash only, delete branch on merge"
gh api --method PATCH "repos/$REPO" \
  -F allow_squash_merge=true -F allow_merge_commit=false -F allow_rebase_merge=false \
  -F delete_branch_on_merge=true -F allow_auto_merge=true >/dev/null
echo "    done"

# --- Rulesets, if the plan allows them ---------------------------------------------------------
echo "==> branch ruleset for the default branch"
LIST_STATUS=$(gh api "repos/$REPO/rulesets" --silent --include 2>&1 | head -1 || true)

if grep -q "403" <<<"$LIST_STATUS"; then
  VISIBILITY=$(gh repo view "$REPO" --json visibility --jq .visibility)
  cat <<WARN
    SKIPPED — GitHub refused (403). This repository is $VISIBILITY on a Free plan.

    Branch protection is free on public repositories. It is private + Free that GitHub does not
    allow. So: make the repository public, or upgrade to Pro (about \$4/month).

    Until you upgrade or make it public, these are enforced by habit and CI rather than by GitHub:
      - open a pull request for every change; do not push to main
      - \`ci\` runs on pushes to main too, so a bad direct push goes red immediately
      - the release workflow only builds tags, and re-runs the whole gate before it does

    The rules are still written down in $RULESET. Run this script again after upgrading and they
    will be applied as specified.
WARN
  exit 0
fi

EXISTING=$(gh api "repos/$REPO/rulesets" --jq '.[] | select(.name=="main") | .id' 2>/dev/null || true)
if [[ -n "$EXISTING" && "$EXISTING" =~ ^[0-9]+$ ]]; then
  gh api --method PUT "repos/$REPO/rulesets/$EXISTING" --input "$RULESET" >/dev/null
  echo "    updated ruleset $EXISTING"
else
  gh api --method POST "repos/$REPO/rulesets" --input "$RULESET" >/dev/null
  echo "    created"
fi

cat <<DONE
==> main now requires:
      - a pull request, squash-merged, approved by a code owner
      - CI green, on a branch that is up to date
      - all review threads resolved
      - no force-push, no deletion
DONE
