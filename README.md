# KMP Arch

Engineering standards for Kotlin Multiplatform projects, delivered as a Gradle plugin so a project
gets all of them from one line and one command.

## Use it

```kotlin
// settings.gradle.kts
plugins { id("io.github.mrlonewolfer.kmp.arch") version "0.7.0" }
```

```bash
./gradlew adoptStandards            # lists every file it would write, and writes nothing
./gradlew adoptStandards --apply    # writes them
```

No GitHub account, no repository access, no npm. A Gradle build is the only requirement.

## What a project gets

| | |
|---|---|
| `CLAUDE.md` | the contract, ending in `@.claude/project.md` |
| `.claude/rules/` | five rules that load when a matching file is opened |
| `.claude/skills/` | add a feature, change a dependency, capture an intent, write a spec, adopt the standards |
| `.claude/agents/verifier.md` | builds and runs both apps, and reports what it saw |
| `.claude/hooks/` + `settings.json` | refuse to let the documents drift from the code |
| `.claude/reference/` | the feature recipe, the testing conventions, the intent and spec templates |
| `architecture-tests/` | the rules that run as tests — layering, locale parity, documented dependencies and seams |
| `.github/` | `ci`, `release`, `release-cut`, `hotfix`, CODEOWNERS, the PR template, the branch ruleset |
| `tools/` | scaffold a feature module, regenerate the module graph, apply branch protection |

## What it will and will not touch

| Owner | Rule |
|---|---|
| **The plugin** | everything in the table above is replaced on update — do not edit them in a project |
| **The project** | `.claude/project.md` is written once if missing, then never touched, even with `--force` |

A project that needs a shared file to differ records the exception in `.claude/project.md` **with the
reason**, rather than editing a file the next update overwrites. The same exception in a second
project means the shared file is wrong; fix it here.

`.standards-ignore` opts a path out entirely — one glob per line, `#` for comments.

`adoptStandards` leaves a file this project has edited alone and says so. `--force` takes the shared
version back.

## Starting a new app

Clone [KMP-Template](https://github.com/mrlonewolfer/KMP-Template) instead. It is a working, tested
app — both platforms building, eight end-to-end scenarios passing on each — and it already contains
these standards. This plugin is for projects that exist, and for updating the standards later.

## Keeping the bundle honest

`standards/` is a copy of files that live, and are exercised by a real build, in the template.
A copy drifts, so it is refreshed by a script rather than by hand:

```bash
tools/sync-from-template.sh              # refresh from ../KMP Template
tools/sync-from-template.sh --check      # fail if anything is stale
```

The plugin's resources are mirrored from `standards/` by the same script, and `--check` compares
those too. `VERSION` is the single version, read by the plugin build.

## Releasing

```bash
tools/sync-from-template.sh --check
echo "0.8.0" > VERSION
cd gradle-plugin && ./gradlew publishPlugins
```
