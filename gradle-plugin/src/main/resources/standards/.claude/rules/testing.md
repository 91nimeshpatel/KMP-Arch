---
paths:
  - "**/*Test.kt"
  - "**/*Spec.kt"
  - "**/commonTest/**"
  - "**/androidHostTest/**"
  - "**/iosTest/**"
  - "architecture-tests/**"
  - "tools/e2e/**"
---

# Testing

Loaded whenever you touch a test. `docs/TESTING.md` has the detail: which source set runs where,
how to run the Appium suite, and the traps that make a test silently not run.

| What | Where | Runs on |
|---|---|---|
| ViewModel, use case, repository, mappers | `commonTest` of the owning module | JVM and iOS |
| Screen (given a `UiState`) | `commonTest` of the feature | JVM and iOS |
| Architecture rules | `:architecture-tests` | JVM |
| Full journeys | `tools/e2e` (Appium) | a device or emulator |

- **Tests are part of the change**, not a follow-up. A bug fix adds a test that fails before it.
- **Fakes over mocks.** One fake per interface, shared from `:core:testing`. A mock asserts how the
  code is written; a fake asserts what it does, and survives refactoring.
- Tests live in `commonTest`, so they run for every platform the app ships to.
- Never `Thread.sleep`. Use `runTest` and the injected test dispatchers.
- E2E finds elements by `TestTags` constants, never by coordinates or translated text. A change to a
  screen updates its page object in the same pull request.
