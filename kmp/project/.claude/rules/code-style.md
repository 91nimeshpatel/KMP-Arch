---
paths:
  - "**/*.kt"
  - "**/*.kts"
  - "**/strings.xml"
  - "**/composeResources/**"
---

# Code rules

Loaded whenever you touch Kotlin or a string resource. `LocalisationTest` in
`:architecture-tests` enforces the string rules; the rest are enforced by ktlint, detekt and
review.

- **No hard-coded user-visible strings.** They go in the owning module's
  `composeResources/values/strings.xml` — a feature's own, or `:composeApp`'s for the navigation
  bar's labels — with every locale updated in the same change. On Android a new
  `composeResources` folder must also be added to `composeResourceModules` in
  `androidApp/build.gradle.kts`, or the screen using it crashes at runtime.
- **A string that must not be translated is still a resource.** Brand names, and the language names in the
  language picker ("Español" stays "Español" in every language), are declared in the default
  `values/strings.xml` with `translatable="false"` and left out of the other locales. Writing them
  as Kotlin literals instead is the same defect as any other hard-coded string: it puts user-visible
  text where no translator, reviewer or tool will ever look at it.
- **No magic values.** Ids, keys and URLs are `const val`; choices are `enum` or `sealed`.
- **`UiState` carries `UiText`, never a resolved `String`.** The ViewModel has no locale; the
  composable resolves the text at render time, so switching language updates it.
- **Design tokens only.** No `Color(0x...)`, no `.dp` literal, no font size in a feature. Read
  `MaterialTheme.*` or `AppTheme.*`. Primitives are `internal` to `:core:designsystem`.
- **Right-to-left by construction.** `start`/`end`, never `left`/`right`.
- **Copy the worked example, do not remember it.** Before writing a screen, a ViewModel or a screen
  test, open `feature/welcome` and read the equivalent file. Imports and APIs move — `AppTheme` is
  under `.theme`, screen tests use `compose.ui.test.v2.runComposeUiTest` — and a remembered import
  is a guess. This is not a style preference: the code generator in `tools/new-feature.py` was
  written from memory and was wrong three ways, each of which was already answered in the file it
  should have been reading.
- **Every public declaration has KDoc** saying what it is for and, where it is not obvious, why it
  is built this way. Comments explain *why*; the code already says *what*. An `actual` inherits
  the `expect`'s KDoc, so it only documents what is specific to that platform — but it does
  document that.
- Zero compiler warnings: the build runs with `-Werror` (see `KmpLibraryConventionPlugin` for the
  one documented exception).
