---
paths:
  - ".github/**"
  - "**/*.md"
  - "tools/release/**"
---

# Branches, commits and releases

Loaded when you touch CI or documentation. `docs/CONTRIBUTING.md` has the pull-request template
and what a reviewer checks.

- `main` is the only long-lived branch and is always releasable. No direct pushes: every change is a
  pull request with CI green, squash-merged, branch deleted on GitHub too (check with
  `git ls-remote --heads origin <branch>`; turn on the repository's "Automatically delete head
  branches", because `gh pr merge --delete-branch` has not always removed the remote branch).
- Branch names: `feat/…`, `fix/…`, `chore/…`, `docs/…`, `ci/…`, `refactor/…`, `test/…`, `perf/…`,
  followed by a short kebab-case description. Example: `feat/offline-sync-retry`.
- **Conventional Commits** for commit and pull-request titles: `feat: add offline retry`. Use `!`
  for a breaking change. CI rejects a title that does not match.
- A pull-request description says what changed, why, and **what was verified and how**.
- Releases (`docs/RELEASE_PLAN.md`, `.claude/skills/release-a-version`): `release/X.Y` is cut from
  `main`; fixes land there by pull request and it is merged back. The tag `vX.Y.Z` on that branch is
  the source of truth for the version; versionName and versionCode / iOS build number derive from it,
  never typed by hand.
- **A store build is made only by `tools/release/release.sh <tag>`**: it checks the branch, the tag
  and the E2E run, keeps every artifact in `../releases/<tag>/` and publishes the GitHub Release. The
  `release-guard.py` hook blocks store builds outside it. A project with several apps (flavors) names
  the app in the branch and tag (`release/us-1.4`, `us-v1.4.0`) and the script builds that flavor.
- **Secrets never enter this repository.** No keystore, no `google-services.json`, no API key. They
  belong in a private vault repository and are read by CI from secrets.
