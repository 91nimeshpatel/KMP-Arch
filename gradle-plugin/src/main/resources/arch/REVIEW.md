# Review instructions

How a pull request in this repository is reviewed, by a person or by Claude. The mechanical rules
are already enforced by `./gradlew qualityCheck` — do not spend review on what a test already
catches. Review is for the things a machine cannot judge.

## Passes

Run three passes and tag every finding with its pass.

### Bugs
Logic errors, broken edge cases, subtle regressions. In this codebase, look hardest at:
- **Runtime wiring.** Koin resolves at runtime, so a new class with a new dependency that was not
  added to a module is a crash on that screen, not a compile error.
- **Resources.** A new module with `composeResources` that is not in `composeResourceModules` in
  `androidApp/build.gradle.kts` crashes on first draw, on Android only.
- **Coroutines.** A swallowed `CancellationException`, a dispatcher read inside a class instead of
  injected, `GlobalScope`, or a `Flow` collected without lifecycle awareness.
- **One-shot state.** A `Channel` of UI events — it drops events whenever the UI is not collecting.

### Platform parity
This is a multiplatform repository, and the most expensive defects are the asymmetric ones.
- Does anything added here work on **both** platforms, or only the one the author ran?
- Is a new `expect`/`actual` justified, and is it inside `:core:platform` (or a documented
  exception)?
- Does an Android-only dependency appear in `commonMain`? That fails at the iOS link step, far from
  the cause.

### Compliance with the written rules
The change matches `CLAUDE.md`, the files in `.claude/rules/`, and the documents in `docs/`.
- Does a dependency change update `docs/LIBRARIES.md` — version, precautions, "On upgrade"?
- Does a new platform seam appear in the table in `docs/ARCHITECTURE.md`?
- Has the change made any document **wrong**? A stale document is a defect, not a formatting issue.

## What Important means here

Reserve **Important** for a finding that would break behaviour, lose user data, leak information,
crash on one platform, or breach a rule in `CLAUDE.md`. Everything else is a nit.

A finding is only Important if you can state the failure concretely: the input, the state, and what
the user sees. "This could be cleaner" is a nit no matter how strongly you feel it.

## Cap the nits

At most five nits per review; summarise the rest as a count. A review of thirty nits and one real
bug hides the bug.

## Do not report

- Anything `ktlint`, `detekt`, `:architecture-tests` or the Konsist rules already enforce — if it
  merged, the gate passed, and duplicating it wastes the author's attention.
- Generated code: `**/build/**`, `composeResources` accessors, Room's `_Impl` classes.
- Formatting. The formatter owns it.

## Verification is part of the review

The pull-request description says what was **run**, not what was intended. A UI change with no
evidence that either app was launched is incomplete — Room, Koin and compose-resources all fail at
runtime, so "it builds" proves very little here. Ask for the evidence rather than assuming it.

## When a finding repeats

If review flags the same mistake twice, the correction belongs in `CLAUDE.md` or `.claude/rules/`,
added in that same review — and, if a machine could check it, in `:architecture-tests` as well.
A review that keeps catching the same mistake is a treadmill. A review that stops it happening
again is worth far more.
