---
paths:
  - "gradle/libs.versions.toml"
  - "**/build.gradle.kts"
  - "build-logic/**"
  - "gradle.properties"
---

# Dependencies, versions and the toolchain

Loaded whenever you touch a build file or the version catalog. **Before changing any dependency,
read its entry in `docs/LIBRARIES.md`** — it records how this project uses it and what fails
silently.

- **`gradle/libs.versions.toml` is the only place a version is written.** A module never writes one.
- **No BOMs.** A `version.ref` already gives a family one version, and this project declares every
  artifact of every family itself. A BOM would only add value for versions arriving *transitively*.
  Add one if that day comes, and say why in the catalog.
- Compose Multiplatform needs no BOM: its Gradle plugin supplies `compose.*` at the plugin version.
- A module's build file is a plugin id, its `android { }` identity, and its project dependencies —
  no versions, no compiler flags. Shared setup lives in `build-logic/`.
- `api` only for types that appear in a module's public signatures; `implementation` for everything
  else, so internals do not leak upward and rebuilds stay small.
- Before adding a dependency: does the standard library or an existing one already do this?

### Every library has a playbook entry

`docs/LIBRARIES.md` records, per dependency: why it is here, **how this project uses it**, the
precautions that will otherwise cost someone a day, and what to re-verify on upgrade.

1. **Read the entry before you touch the library.** It is where this repo's hard-won facts live —
   which API we chose, which we avoid, and what fails silently. Do not rediscover them.
2. **Adding a dependency adds its entry, in the same commit.** An entry written later is an entry
   written from memory.
3. **Upgrading a version updates its entry, in the same commit.** Work through that entry's
   "On upgrade" line and re-verify each item. If a precaution no longer applies, delete it and say
   so in the pull request; a stale warning sends the next person the wrong way.
4. **Removing a dependency removes its entry**, and any workaround it justified.
5. **Cross-check against reality.** Before writing an entry, read how the library is actually used
   in this repo and compare it with the library's current documentation. Where the two disagree,
   the entry says so explicitly rather than repeating the documentation.
6. **Record the fix where the fact was learned.** When a library surprises you — a silent failure, a
   version floor, a platform-only gotcha — the entry is updated in the change that found it, with
   the symptom, so the next person recognises it from the symptom rather than the cause.

An entry documents *our use*, not the library. Never paste its README.

`LibraryPlaybookTest` in `:architecture-tests` fails the build when a catalog entry has no section
in `docs/LIBRARIES.md`, so this rule is checked, not trusted.

## Toolchain facts (verified 2026-09-24 — re-verify before changing)

- AGP 9.4.1 with built-in Kotlin. Do **not** apply `org.jetbrains.kotlin.android`; AGP 9 brings its
  own. Gradle 9.8.0, Kotlin 2.4.20, KSP 2.3.12, JDK 17, `compileSdk`/`targetSdk` 37, `minSdk` 24.
- Compose Multiplatform 1.12.1. It is mid-migration between the JetBrains `org.jetbrains.androidx.*`
  ports and Google's multiplatform `androidx.*` artifacts, which is why two things look odd and are
  deliberate: the `compose.*` accessors are used with `@Suppress("DEPRECATION")` (material3 has no
  1.12.1 release), and `-Werror` is off for metadata compilations. Both carry a comment in
  `build-logic/` saying when to delete them. Re-test on every upgrade.
- An application module cannot declare `androidTarget()` under AGP 9's new DSL, which is why
  `:androidApp` and `:composeApp` are separate.
- Apple targets are `iosArm64` and `iosSimulatorArm64`. There is no `iosX64`: Compose Multiplatform
  no longer publishes Intel-simulator artifacts, and declaring it breaks resolution.
- **Never add `gradle/gradle-daemon-jvm.properties` or `jvmToolchain()`.** Android Studio's Gradle
  client cannot provision toolchains on this machine and sync fails with "Service 'SystemInfo' is
  not available". The build uses whatever JDK 17 starts Gradle. `updateDaemonJvm` writes that file,
  so it is git-ignored as well as banned.
- Compose Multiplatform 1.12.1 does not package a KMP library's resources into an Android app under
  AGP 9, so `:androidApp` copies them into its assets itself. The task carries the explanation and a
  note to delete it once the upstream bug is fixed.
