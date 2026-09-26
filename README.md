# KMP Arch

Engineering standards for Kotlin Multiplatform projects, installed by one Gradle task.

Most shared standards are a document someone has to remember to read. These are files in your
repository that do something: rules that fail the build, CI workflows, a generator for new feature
modules, and the instructions an AI agent reads before it touches your code.

## Use it

```kotlin
// settings.gradle.kts
plugins { id("io.github.91nimeshpatel.kmp.arch") version "0.7.3" }
```

```bash
./gradlew adoptArch            # lists what it would write, writes nothing
./gradlew adoptArch --apply    # writes it
```

Run it again whenever you bump the version; it updates what it owns and leaves the rest alone.

## What you get

| | |
|---|---|
| **`architecture-tests/`** | Seven rules that run as tests and fail the build: layer dependencies, feature isolation, locale parity, hard-coded strings, undocumented dependencies, undocumented platform seams, and a module diagram that must match the build files. |
| **`tools/new-feature.py`** | Scaffolds a feature module — state, events, ViewModel, a screen split from its route, a string in every locale you ship, a unit test, an end-to-end scenario, and the three registrations a new module needs. |
| **`.github/workflows/`** | `ci`, `release`, `release-cut`, `hotfix`, plus CODEOWNERS, a PR template and a branch ruleset. |
| **`.claude/`** | Rules that load when an agent opens a matching file, four skills, a verifier subagent, and hooks that refuse to end a turn while the docs contradict the code. |
| **`CLAUDE.md`, `REVIEW.md`, `docs/`** | The contract, what a review looks for, and the documents the skills point to. |

## How it helps

**The rules are executable.** A layering violation, a missing translation or a hard-coded string
fails the build rather than waiting for someone to notice in review.

**New modules start correct.** The scaffolder writes the registrations people forget — including the
one whose absence crashes the app at runtime with no build error.

**Agents follow the same rules you do.** The `.claude/` files mean an AI working in your repository
reads your architecture and your conventions, not its own defaults.

**Updates are one command.** Bump the version, run `adoptArch` again.

## What happens on first run

The rules run immediately, and **some will fail** — that is them working, not them broken.

Seven of the twenty check documents that belong to *your* project and cannot be shipped with the
plugin: `docs/ARCHITECTURE.md` describing your modules and the decisions behind them, and
`docs/LIBRARIES.md` describing what each of your dependencies is for and what breaks on upgrade.
Shipping one project's copies of those would be confidently wrong in every other project, so the
rules ask you to write them and name exactly what is missing.

The other thirteen — layering, feature isolation, hard-coded strings, the generated module diagram,
rule references — pass immediately.

The architecture tests are a Gradle module, so they also need entries in your version catalog and an
`include` in `settings.gradle.kts`. `adoptArch` prints the exact lines when they are absent.

## What it will not touch

| Owner | |
|---|---|
| **The plugin** | everything above is replaced on update — do not edit those files in your project |
| **You** | `.claude/project.md` is created once if missing, then never touched, even with `--force` |

If a shared file does not fit, record the exception in `.claude/project.md` with the reason rather
than editing a file the next update overwrites.

`adoptArch` leaves a file you have changed alone and tells you which. `--force` takes the shared
version back. `.arch-ignore` opts a path out entirely — one glob per line.

## Requirements

Gradle 8+, JDK 17+.

The architecture tests assume a Kotlin Multiplatform project laid out as `core/*` and `feature/*`
modules. In a project shaped differently they will report violations that are not violations, so
read them before wiring them into your build gate.

## Licence

MIT — see [LICENSE](LICENSE).

Copyright © 2026 Nimesh Patel. You may use, modify and distribute this freely, including
commercially, provided the copyright notice and licence text travel with it.
