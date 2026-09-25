---
name: change-a-dependency
description: Add, upgrade or remove a dependency in this Kotlin Multiplatform project. Use whenever a library, plugin or version in gradle/libs.versions.toml changes, or when asked to bump, add or drop a dependency.
---

# Changing a dependency

A dependency change is never one edit. The version catalog is the smallest part of it; the part that
decays is the knowledge about *why* the version is what it is and what breaks when it moves.

Work through this in order. Do not start at step 3.

## 1. Read before you touch

Open your project's `docs/LIBRARIES.md` and find the entry for the library.
If you are writing a new entry, `@../../reference/library-entry-format.md` is the shape it takes. It records how **this project** uses
it, the precautions that fail silently, and what to re-verify on upgrade. Rediscovering those costs
a day; reading them costs a minute.

If there is no entry and the library is already in the catalog, that is a defect — say so.

## 2. Decide, and say why

- **Adding?** Can the standard library or something already here do this? Does it work on Android
  *and* iOS? An artifact that resolves only on the JVM fails at the iOS link step, not at the call
  site.
- **Upgrading?** Read the release notes for the versions you are skipping, not just the newest.
- **Removing?** Everything the entry justified goes too: workarounds, suppressions, exclusions.

## 3. Change the catalog — and only the catalog

`gradle/libs.versions.toml` is the single place a version is written. A module's build file gets a
plugin id, its `android { }` identity and its project dependencies. Never a version.

No BOMs: a `version.ref` already gives a family one version, and this catalog declares every
artifact it uses.

## 4. Update `docs/LIBRARIES.md` in the same commit

This is the step that gets skipped, so it is the step that is checked.

- **Version** line matches the catalog exactly. `LibraryPlaybookTest` fails the build otherwise.
- **Precautions** — work through each one and ask whether the new release changes it. A precaution
  that no longer applies is *deleted*, and the pull request says so; a stale warning sends the next
  person the wrong way.
- **On upgrade** — do what it says, then update it if the answer changed.
- **New library?** Write the whole entry: why it is here, how this project uses it, the precautions,
  and what to re-verify. Document *our use*, never paste the README.
- **Removed?** Delete the entry, and add the name to `KNOWN_REMOVED` in `LibraryPlaybookTest` so it
  cannot quietly come back.

## 5. Verify on both platforms

```bash
./gradlew qualityCheck          # includes the playbook and architecture rules
./gradlew allTests              # JVM and iOS
./gradlew :androidApp:installDebug
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'generic/platform=iOS Simulator' -derivedDataPath build/ios CODE_SIGNING_ALLOWED=NO
```

A dependency change that only compiles is not verified. Room, Koin, DataStore and compose-resources
all fail at **runtime**, on the screen that uses them — so launch both apps and look.

## 6. Report

Say which versions moved, which precautions changed, and what you ran. If you could not verify
something, say that instead of implying you did.
