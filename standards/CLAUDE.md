<!--
  This file is installed by `./gradlew adoptStandards` and is REPLACED on every update.
  Do not edit it. Anything specific to this project goes in .claude/project.md, which the plugin
  never writes and never reads back.
-->

# Engineering rules

These rules are binding for every agent and every person working in this repository. They are not
suggestions. An implementation that skips a rule is incomplete and must not be merged.

This file is the contract. When it and a habit disagree, this file wins. When it and the official
documentation of a library disagree, stop and report the conflict instead of guessing.

---

## Start here: read `docs/` before you touch anything

**This file is loaded for you automatically, and so are the path-scoped rules in `.claude/rules/`
whenever you open a file they cover. The five documents in `docs/` are not — open and read them
yourself, at the start of the task, before writing any code.** They are what tells you what this
project is, how it is built, and what has already been decided. An agent that skips them re-decides
settled questions, reinvents the recipe, and rediscovers traps that cost someone else a day.

Read all five on any non-trivial task. On a one-line fix, read at least the one that governs the
file you are changing.

| Document | Read it to learn |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | what the app *is*: the layers, the module graph, every platform seam, and **why** each decision was made rather than its alternative |
| [`docs/LIBRARIES.md`](docs/LIBRARIES.md) | every dependency: how this project uses it, the precautions that fail silently, and what to re-verify on upgrade |
| [`docs/ADDING_A_FEATURE.md`](docs/ADDING_A_FEATURE.md) | the step-by-step recipe for a screen, module, ViewModel or string — follow it rather than inventing one |
| [`docs/TESTING.md`](docs/TESTING.md) | what to test, which source set it belongs in, and what will silently not run |
| [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) | branch names, commit and pull-request format, and what a reviewer will check |

### Cross-check them against the code, and against each other

Reading is not enough — **verify**. These documents are maintained by hand and can drift:

1. **Document against code.** If a document describes something the code no longer does, **stop and
   say so.** Do not quietly follow either one. One of the two is wrong, and picking silently is how
   a repository ends up with documentation nobody trusts.
2. **Document against document.** They overlap on purpose — the module graph appears in
   `docs/ARCHITECTURE.md`, `README.md` and `.claude/rules/architecture.md`. If those disagree, that
   is a defect to report, not a detail to work around.
3. **Document against the rules.** Where `docs/` conflicts with this file or a file in
   `.claude/rules/`, **the rule wins** — and the document gets fixed in your change.

### Where the rest of the rules live

This file is the short universal contract. The detail is split into path-scoped rules that load
themselves when you open a file they cover, so every agent gets the same knowledge whether or not it
thought to go looking:

| Rule file | Loads when you touch |
|---|---|
| `.claude/rules/architecture.md` | `core/**`, `feature/**`, `composeApp/**`, `androidApp/**` |
| `.claude/rules/dependencies.md` | the version catalog, any `build.gradle.kts`, `build-logic/**` |
| `.claude/rules/code-style.md` | any `.kt`/`.kts`, any `strings.xml`, any `composeResources/**` |
| `.claude/rules/testing.md` | any test source, `architecture-tests/**`, `tools/e2e/**` |
| `.claude/rules/contributing.md` | `.github/**`, any `.md` |

Procedures are **skills**, which load when the work matches them, and cost nothing until then:

| Skill | Use it when |
|---|---|
| `.claude/skills/change-a-dependency` | adding, upgrading or removing any library, plugin or version |
| `.claude/skills/add-a-feature` | adding a screen, module, ViewModel or user-visible string |
| `.claude/skills/capture-intent` | someone describes a want or a problem and no `intent.md` exists yet |
| `.claude/skills/write-a-spec` | an intent is accepted and someone asks what it would take to build |

One **subagent** is defined, in `.claude/agents/verifier.md`. It builds, installs and runs both apps
and reports what it saw. Use it before calling any UI or wiring change done — its verdict is worth
having precisely because it did not write the code.

Two **hooks** enforce what prose cannot, configured in `.claude/settings.json`:

| Hook | Fires |
|---|---|
| `catalog-changed.py` | after you edit `gradle/libs.versions.toml`, naming the versions now missing from `docs/LIBRARIES.md` |
| `docs-gate.py` | when you try to finish a turn while a document contradicts the code |

The division is deliberate: **a skill makes a mistake rare, a test makes it visible, and a hook makes
it close to impossible.** Anything a machine can check is checked (§1.10).

Three things are true of every task, and they are the ones most often skipped:

1. **A library is never used from memory.** Read its entry in `docs/LIBRARIES.md` first. Adding or
   upgrading one updates that entry in the same commit — the version, the precautions and the
   "On upgrade" line. `LibraryPlaybookTest` fails the build if you forget.
2. **The documents are updated with the code, never after it.** §5 lists which document each kind of
   change touches.
3. **Run `./gradlew qualityCheck` before calling anything done.** Thirteen of these rules are
   executable tests. It takes seconds, and it is the difference between believing and knowing.

---

## 1. How to work (process)

1. **Never assume, never trust blindly.** Before changing anything, read the *actual* code, config
   or documentation involved. Do not rely on memory, file names or an earlier message when the
   source is available. If a claim cannot be verified from a primary source, say so instead of
   presenting it as fact.
2. **Read the full context.** Read every file the change touches, end to end — not a `grep` hit,
   not the first 100 lines. Read the callers and the callees.
3. **Read the documents before you write the code.** `docs/` is not background reading; it is where
   this repository's decisions and traps already live, and re-deciding something it settled is
   wasted work at best and a contradiction at worst. Before starting, read the ones that apply:

   | Before you… | Read |
   |---|---|
   | touch any library, or change a version | `docs/LIBRARIES.md` — its precautions and its "On upgrade" line |
   | add a screen, module, ViewModel or string | `docs/ADDING_A_FEATURE.md` — the recipe, then follow it |
   | change a layer, a seam or a dependency direction | `docs/ARCHITECTURE.md` — and the reasoning, not just the diagram |
   | write or move a test | `docs/TESTING.md` — what goes where, and what will not run |
   | open a pull request | `docs/CONTRIBUTING.md` — branch, title and description format |

   If what you find in the code contradicts what a document says, **stop and report it** rather than
   picking one. One of the two is wrong, and guessing which is how a repository ends up with
   documentation nobody trusts.
4. **Cross-check before deciding.** Compare the code against the task, against the current official
   documentation of the library involved, and against this file. Where they disagree, stop and
   report it.
5. **Find the optimal solution, then prove it.** For any non-trivial change, list the viable
   approaches with their trade-offs, pick one, and say why the others lost. Only then implement.
6. **Never break working behaviour.** Keep existing behaviour unless changing it is the task.
   "It compiles" is not verification; §6 is.
7. **When stuck or ambiguous, ask.** If two readings of a task lead to materially different work,
   ask a precise question. Do not guess, do not silently pick, do not build both.
8. **Leave nothing stale.** No unused code, no commented-out blocks, no dead dependency, no
   documentation that describes something the code no longer does. Deleting is part of the change.
9. **Report faithfully.** State what was verified and how, and what was not. A failing test is
   reported as failing, with its output.
10. **A rule that can be checked, is checked.** Every rule in this file that a machine could verify
   belongs in `:architecture-tests`, not only in prose. When a rule is broken — by anyone, including
   you — the fix is two changes: correct the code, *and* add the check that would have caught it.
   A rule enforced only by good intentions is a rule that will be skipped under pressure, and the
   evidence is in this repository's own history: the rules that were violated were the unchecked
   ones, while no unchecked-for-the-first-time violation survived once a test existed.

## 2. What this template is

A Kotlin Multiplatform app that runs on Android and iOS from one codebase, built to Google's
recommended architecture. Everything the user sees — screens, navigation, design tokens,
localisation — is Compose Multiplatform in `commonMain`. Only genuine platform differences are
written twice.

Clone it, rename it (§10), and the first feature you add has a place to go.

## 3. Verifying your work

Every command, with what a healthy run looks like. Run them before reporting anything complete, and
**paste the output** — the evidence comes from the toolchain, not from your confidence.

| Command | Healthy output |
|---|---|
| `./gradlew qualityCheck` | `BUILD SUCCESSFUL` — ktlint, detekt, 16 architecture rules, coverage floor |
| `./gradlew allTests` | `BUILD SUCCESSFUL` — unit and screen tests, JVM and iOS simulator |
| `./gradlew :androidApp:installDebug` | `BUILD SUCCESSFUL`, then the app launches and draws |
| `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` | `BUILD SUCCESSFUL` |
| `tools/e2e/run.sh` | `8 passed` (or more) — for any change to a screen or a flow |

**If a test fails, fix the code, not the test.** Never delete, skip or weaken a check to go green;
if a check is genuinely wrong, say so in the pull request and change it deliberately.

**A green build is not verification here.** Koin resolves at runtime, Compose resources are packaged
separately on Android, and iOS validates its plist at launch — all three fail on screen, not at
compile time. Launch both apps and look, or hand it to the `verifier` subagent.

## 4. Running the apps

```bash
# Android: an emulator or a device, then
./gradlew :androidApp:installDebug

# iOS: open the Xcode project and press Run
open iosApp/iosApp.xcodeproj
```

Xcode builds the Kotlin framework itself through a build phase, so no Gradle command is needed
first. The very first iOS build is slow because Kotlin/Native compiles the whole dependency graph.

## 5. Definition of done

A change is done when: the task is fully implemented; every check in §7 passes; tests exist for new
logic; strings are localised in every locale; KDoc is written; nothing stale is left behind; and the
pull-request description says what was verified.

**And the documents match the code again.** "Update the docs" is too vague to act on, so it is a
checklist. Work through it and tick only what you actually checked:

| If the change… | Then update |
|---|---|
| adds, upgrades or removes a dependency | `docs/LIBRARIES.md` — the entry, its precautions and its "On upgrade" line. Enforced by `LibraryPlaybookTest`. |
| adds an `expect`/`actual` seam | `docs/ARCHITECTURE.md` — a row in the platform-seam table. Enforced by `SeamDocumentationTest`. |
| adds or removes a module, or changes who depends on whom | `docs/ARCHITECTURE.md` (module graph), `README.md` (Mermaid chart), `CLAUDE.md` §3 (dependency table) |
| changes a rule, or how something must be built | `CLAUDE.md`, and a check in `:architecture-tests` if a machine could verify it (§1.10) |
| adds a capability a newcomer would look for | `README.md` — the "What is in the box" table |
| changes where tests live or how they run | `docs/TESTING.md` |
| changes the recipe for a new screen | `docs/ADDING_A_FEATURE.md` |

A document that has drifted is worse than no document: the next person trusts it, and it lies to
them. If a change makes a document wrong and you cannot fix it now, say so in the pull request
rather than leaving it.

---

## This project

@.claude/project.md
