# KMP Arch

Engineering standards for Kotlin Multiplatform projects, installed by one Gradle task.

Most shared standards are a document somebody has to remember to read. These are files in your
repository: rules that run as tests and fail the build, CI workflows, the instructions an AI agent
reads before it touches your code, and a generator for new feature modules. One command installs
them, the same command updates them later, and neither one overwrites work you have done.

## Use it

```kotlin
// settings.gradle.kts
plugins { id("io.github.mrlonewolfer.kmp.arch") version "0.7.1" }
```

```bash
./gradlew adoptArch            # lists every file it would write, and writes nothing
./gradlew adoptArch --apply    # writes them
```

That is the whole setup. No account, no repository access, no other tool.

## What lands in your project

| | |
|---|---|
| `architecture-tests/` | Seven rules that run as JVM tests: layer dependencies, feature isolation, locale parity, hard-coded strings, undocumented dependencies, undocumented platform seams, a module graph that must match the build files. They fail the build, not a review. |
| `.claude/rules/` | Five rules that load themselves when an agent opens a file they cover — architecture, code style, dependencies, testing, contributing. |
| `.claude/skills/` | Add a feature, change a dependency, capture an intent, write a spec. |
| `.claude/agents/verifier.md` | A subagent that builds and runs both apps and reports what it actually saw. |
| `.claude/hooks/` | Refuse to end a turn while a document contradicts the code, and name the versions missing from the dependency playbook the moment the catalog changes. |
| `.github/workflows/` | `ci`, `release`, `release-cut`, `hotfix`, plus CODEOWNERS, a pull-request template and a branch ruleset. |
| `tools/new-feature.py` | Scaffolds a feature module — state, events, ViewModel, a screen split from its route, a string in every locale you already ship, a test, an end-to-end scenario, and the three registrations a new module needs. |
| `CLAUDE.md`, `REVIEW.md`, `docs/` | The contract, what a review looks for, and the documents the skills send people to. |

## What it will and will not touch

| Owner | Rule |
|---|---|
| **The plugin** | everything above is replaced on update — do not edit those files in your project |
| **You** | `.claude/project.md` is created once if missing, then never touched, even with `--force` |

If a shared file does not fit your project, record the exception in `.claude/project.md` **with the
reason**, rather than editing a file the next update overwrites.

`adoptArch` leaves a file you have changed alone and tells you which. `--force` takes the shared
version back. `.arch-ignore` opts a path out entirely — one glob per line.

## Starting a new app

Use [KMP-Template](https://github.com/mrlonewolfer/KMP-Template) instead: a working Android and iOS
app that already contains these standards, with both platforms building and eight end-to-end
scenarios passing on each. This plugin is for projects that already exist, and for updating the
standards in any project later.

## Requirements

Gradle 8 or later, JDK 17 or later. The architecture tests assume a Kotlin Multiplatform project
laid out as `core/*` and `feature/*` modules; in a project shaped differently they will report
violations that are not violations, so read them before wiring them into your gate.

## Contributing

Issues and pull requests are welcome. The files under `arch/` are copies of files that live, and are
exercised by a real build, in the template — so they are refreshed by `tools/sync-from-template.sh`
rather than edited here, and CI fails when they drift.

## Licence

MIT.
