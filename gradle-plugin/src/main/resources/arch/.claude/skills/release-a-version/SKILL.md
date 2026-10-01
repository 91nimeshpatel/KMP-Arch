---
name: release-a-version
description: Release a version to the stores (App Store, Google Play, TestFlight, Play testing tracks) — cutting release/X.Y, tagging vX.Y.Z, building, uploading and keeping the artifacts. Use whenever anyone asks to release, publish, ship, submit, upload a build, make a TestFlight or Play build, or bump the version.
---

# Releasing a version

`.claude/rules/contributing.md` is the rule and `docs/RELEASE_PLAN.md` the design; this is the order.
The hook `release-guard.py` blocks any store build that does not run through
`tools/release/release.sh`, so there is no shortcut.

1. **Decide the version** with the owner: `X.Y.0` for features, `X.Y.Z` for fixes on an existing
   `release/X.Y`. Releases are batched; never one per fix unless the owner says so.
2. **Release cut.** New minor: the `release-cut` workflow with X.Y, or
   `git branch release/X.Y main && git push origin release/X.Y` where Actions does not run. A patch:
   fixes reach `release/X.Y` by pull request (cherry-pick `-x` from main, or fix there and merge back).
   A project with several apps (product flavors) names the app in branch and tag (`release/us-X.Y`,
   `us-vX.Y.Z`) and adapts the script to build that flavor.
3. **E2E on the release branch**: `tools/e2e/run.sh` on Android and iOS; keep the output as the log.
4. **Tag** the exact commit: `git tag -a vX.Y.Z -m "…" && git push origin vX.Y.Z`.
5. **Build**: `tools/release/release.sh vX.Y.Z --e2e-log <log>` with `KEYSTORE_PROPERTIES` and
   `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_FILE` from the private vault. It derives versionName and
   versionCode from the tag, builds and signs both apps, uploads iOS to App Store Connect, keeps every
   artifact in `../releases/<tag>/` and publishes the GitHub Release with the AAB, mapping, native
   symbols, archive and dSYMs.
6. **Stores**: upload the AAB to the Play track; attach the iOS build to the App Store version;
   release notes; submit only with the owner's yes.
7. **Merge `release/X.Y` back into `main`** by pull request.

Never: build a store binary from `main`, type version numbers by hand, or keep artifacts only in a
temporary folder. Deleting a merged branch: check it is gone on GitHub (`git ls-remote --heads`).
