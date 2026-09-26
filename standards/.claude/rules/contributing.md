---
paths:
  - ".github/**"
  - "**/*.md"
---

# Branches, commits and releases

Loaded when you touch CI or documentation. `docs/CONTRIBUTING.md` has the pull-request template
and what a reviewer checks.

- `main` is the only long-lived branch and is always releasable. No direct pushes: every change is a
  pull request with CI green, squash-merged, branch deleted.
- Branch names: `feat/…`, `fix/…`, `chore/…`, `docs/…`, `ci/…`, `refactor/…`, `test/…`, `perf/…`,
  followed by a short kebab-case description. Example: `feat/offline-sync-retry`.
- **Conventional Commits** for commit and pull-request titles: `feat: add offline retry`. Use `!`
  for a breaking change. CI rejects a title that does not match.
- A pull-request description says what changed, why, and **what was verified and how**.
- Releases: `release/X.Y` is cut from `main`; fixes land there and are merged back. The tag
  `vX.Y.Z` is the source of truth for the version.
- **Secrets never enter this repository.** No keystore, no `google-services.json`, no API key. They
  belong in a private vault repository and are read by CI from secrets.
