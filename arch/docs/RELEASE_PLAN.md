# Release plan

How a change gets from `main` to a user, and how versions are decided. The workflows in
`.github/workflows/` implement this; each one names the section it comes from.

---

## 1. Branch model

`main` is the only long-lived branch and is always releasable. Nothing is pushed to it directly.
`.github/rulesets/main.json` says so, and `tools/apply-branch-protection.sh` applies it.

**One caveat, verified against GitHub rather than assumed:** branch protection is free on **public**
repositories on every plan, but GitHub refuses it — both the rulesets API and the older
branch-protection API — on a **private** repository on the Free plan. If that is you, either make
the repository public or upgrade to Pro. Until then `ci` still runs on pushes to `main`, so a bad
direct push goes red within a minute; that is detection, not prevention, and the difference matters.

| Branch | Cut from | Merges to | Lives |
|---|---|---|---|
| `feat/…`, `fix/…`, `chore/…` | `main` | `main`, squashed | days |
| `release/X.Y` | `main` | back to `main` after each tag | until X.Y is unsupported |
| a hotfix `fix/…` | `release/X.Y` | that release branch, then `main` | hours |

A release branch exists so `main` stays open. Once `release/1.4` is cut, only fixes land on it while
the next feature carries on in `main`.

## 2. Versioning

**The tag is the source of truth.** `v1.4.0` produces `versionName 1.4.0`; `versionCode` (and the iOS
build number) is derived from the version, so it only ever climbs:
`MAJOR*1000000 + MINOR*10000 + PATCH*100 + 99` (1.4.0 → 1040099), exactly as `release.yml` computes it.
Nothing is edited by hand — the values in `gradle.properties`
are just what a local build uses so you never have to touch a version to run the app.

Semantic versioning, read from the user's point of view rather than the code's:

- **Major** — someone's saved data or workflow changes in a way they have to notice.
- **Minor** — new capability, nothing taken away.
- **Patch** — a fix, no new capability.

## 3. Pipelines

| Workflow | Trigger | Does |
|---|---|---|
| `ci` | every pull request and push | lint, 20 architecture rules, tests, coverage, Android build, iOS build, E2E on release branches |
| `release-cut` | manual | creates `release/X.Y` from `main` |
| `hotfix` | manual | creates a fix branch from a release branch and prints the steps |
| `release` | tag `v*`, or manual | derives the version, runs the whole gate again, builds, publishes a **draft** release |
| `tools/release/release.sh` | run locally with the tag | the store build: signs both apps (keys from a private vault), uploads iOS, keeps every artifact, publishes the GitHub Release |

`release` runs the full `ci` gate again rather than trusting an earlier green run. It is the one
build that reaches users, so it earns more checking, not less.

## 4. Release flow

1. `main` is green and you decide X.Y is ready → run **release-cut** with `X.Y` (or, where Actions
   cannot run: `git branch release/X.Y main && git push origin release/X.Y`).
2. Fixes land on `release/X.Y` by pull request. Same checks as anywhere else, and `tools/e2e/run.sh`
   passes on the branch.
3. Tag `vX.Y.0` on that branch and push it (**release** then builds an unsigned draft in CI).
4. Build for the stores with `tools/release/release.sh vX.Y.0 --e2e-log <log>`
   (`.claude/skills/release-a-version`); it publishes the GitHub Release with the artifacts.
5. Upload to the stores and submit.
6. Merge `release/X.Y` back into `main` so the fixes are not lost.

A project with several apps (product flavors) releases each one separately: `release/us-X.Y`, tag
`us-vX.Y.Z`, and the script builds that flavor and its iOS configuration.

### Hotfix

1. Run **hotfix** with the release branch and a short description.
2. **Write the failing test first**, and commit it before the fix. A test that existed before the
   fix, and that nobody rewrote, is the proof the bug is gone.
3. Fix the code until it passes. Do not change the test.
4. Pull request into the release branch, tag `vX.Y.Z+1`, build it with `tools/release/release.sh`,
   merge back to `main`.

A fix made in a hurry is exactly the one that needs the gate, not the one that should skip it.

## 5. Publishing modules as artifacts

**Not done today.** Every `:core:*` module is consumed with `project(":core:x")` inside this build
only. This section exists so that when a second app needs `:core:designsystem`, nobody has to work
out the rules from scratch.

### What is already in place

The expensive part is done. Each module declares `api` for types that appear in its public
signatures and `implementation` for everything else — so `:core:data` exposes `:core:model` and
hides `:core:database`. That distinction is what a published artifact lives or dies by, and it
cannot be retrofitted later without breaking whoever already depends on you.

### What you would add

1. **`maven-publish`** in a new convention plugin, not in each module, so coordinates and POM
   metadata are declared once. Group comes from your package; the version comes from the tag, the
   same way the app's does.
2. **A binary-compatibility check** — `binary-compatibility-validator`. It writes a `.api` file per
   module that you commit, so **changing a public signature shows up as a diff in review** instead
   of as a broken build in somebody else's project. Without this, publishing is guesswork. It is the
   same generate-and-check pattern the module graph already uses.
3. **A rule about what may be public.** Today `internal` is used freely because the only consumer is
   this build. Once published, every `public` declaration is a promise. Audit before the first
   release, not after.

### Versioning published modules

**Version every module together, from the same tag.** Independent per-module versions sound tidier
and are not: consumers then have to work out which combination is compatible, and you have to test
those combinations. One version for the set means `1.4.0` of anything works with `1.4.0` of
everything.

Once published, semantic versioning stops being a judgement and becomes a rule the `.api` files
decide for you:

- Removing or changing a public signature is **major**. No exceptions, however obviously wrong the
  old signature was.
- Adding one is **minor**.
- Changing only behaviour behind an unchanged signature is **patch** — and is the one to be careful
  with, because consumers have no way to see it coming.

Deprecate for at least one minor version before removing anything.

## 6. What this plan does not cover

Signing in CI. The `release` workflow stops at an unsigned draft on purpose: a template must not carry
signing material. Store builds are signed and uploaded locally by `tools/release/release.sh`, which
reads the keys from a private vault; `.github/workflows/release.yml` documents where a keystore and a
store-upload step would plug in for CI.
