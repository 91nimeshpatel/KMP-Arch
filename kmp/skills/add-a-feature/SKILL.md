---
name: add-a-feature
description: Add a new screen, feature module, ViewModel or user-visible string to this Kotlin Multiplatform project. Use when asked to build a screen, add a feature, create a module, or wire something new into navigation.
---

# Adding a feature

`feature/welcome` is the worked example — read it alongside this. The full reasoning is in
`@../../reference/ADDING_A_FEATURE.md`; this is the order of operations and the traps.

## 1. Decide whether you need a module

A module is right for a user-facing area that can stand alone. Two screens that are two tabs of one
thing belong in one module. A single shared class is not a module — it goes in `:core:*`.

## 2. Register it

`settings.gradle.kts`, then a build file that is **a plugin id, a namespace and project
dependencies — nothing else**. No versions, no compiler flags: `template.kmp.feature` already brings
Compose, `:core:designsystem`, `:core:ui`, `:core:model`, `:core:domain`, `:core:testing`, lifecycle
and Koin.

## 3. State, events, ViewModel, screen — in that order

- One immutable `UiState`; one `sealed interface` of events; **one read-only `StateFlow`**.
- **No `Channel` of one-shot events.** They are dropped whenever the UI is not collecting. Put the
  outcome in the state and clear it once shown.
- `stateIn(WhileSubscribed(5_000))` — outlives a rotation, stops work when the user leaves.
- The ViewModel imports **no** Android and **no** Compose. `LayerDependencyTest` fails the build.
- Split `XRoute` (gets the ViewModel) from `XScreen` (pure function of its state). That split is
  what makes the screen testable and previewable.

## 4. Strings, and the two traps

Every user-visible string goes in `composeResources/values/strings.xml` **and every locale**, in the
same change. `LocalisationTest` enforces parity and fails on a literal in Kotlin.

1. **A new module with `composeResources` must be added to `composeResourceModules` in
   `androidApp/build.gradle.kts`**, or the screen crashes at runtime with `MissingResourceException`
   — Compose Multiplatform does not package a KMP library's resources under AGP 9. Checked by
   `LocalisationTest`, but know why it exists.
2. A string that must **not** be translated — a brand name, a language's own name — is still a
   resource: `translatable="false"`, default `values/` only.

## 5. Tokens only

`AppTheme.spacing.medium`, `MaterialTheme.colorScheme.primary`. A `.dp` literal or a `Color(0x…)` in
a feature is a defect; the design-system primitives are `internal` so you cannot reach them. Use
`start`/`end`, never `left`/`right`.

## 6. Data, only if the feature needs new data

Work upward: data source → model in `:core:model` → repository in `:core:data` → *only then* a use
case, and only if it is shared by two ViewModels or combines two repositories. A use case that
forwards one call is noise. A feature that reaches past its repository fails the build.

## 7. Test tags

`TestTags` constants on the root and anything E2E touches. A `Dialog` or `Popup` is its **own
window** and publishes nothing unless you add `Modifier.exposeTestTagsToAutomation()` to it — the
element is visible on screen and invisible to Appium.

## 8. Wire it up

Koin module in `composeApp/di/AppModules.kt` — Koin resolves at runtime, so a missing binding is a
crash on the screen, not a compile error. Then the navigator, if it is a top-level destination; the
`when` is exhaustive so the compiler will not let you forget the screen.

## 9. Verify

```bash
./gradlew qualityCheck && ./gradlew allTests
```

Then **run both apps and look at the screen**. Use the `verifier` subagent if you want that done
thoroughly. Update the docs your change touched — CLAUDE.md §5 lists which.
