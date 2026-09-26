# Testing

What to test, where it goes, and how to run it. These rules are binding
([CLAUDE.md](../CLAUDE.md) §6).

---

## 1. Where a test goes

The binding table lives in [`.claude/rules/testing.md`](../.claude/rules/testing.md), which loads
itself whenever you open a test — one copy, so it cannot drift from what this page says.

The rule of thumb behind it: **put the test in `commonTest`.** A test there runs on every platform
the app ships to, so it also proves the code compiles and behaves on iOS. Reach for a
platform-specific source set only when the thing under test exists only there.

```
./gradlew allTests              # every module, every platform
./gradlew testAndroidHostTest   # the fast loop: the same commonTest code, on the JVM only
```

`testAndroidHostTest` is the JVM run of `commonTest`. It is what CI's Linux job runs, because the
Apple targets need a macOS runner; `allTests` adds them and is what you run before opening a pull
request on a Mac.

## 2. What to test

Test behaviour that can break: state transitions, error paths, mapping, ordering, caching, edge
cases. Do not test that a data class stores what you passed it.

Every change that adds or changes logic ships with its tests. A bug fix ships with a test that
fails before the fix — otherwise nothing stops the bug coming back.

### A ViewModel test

Assert the sequence of states, not the internals:

```kotlin
@Test
fun `refresh failure surfaces a message and stops loading`() = runTest {
    repository.failNextRefresh()
    viewModel.uiState.test {
        awaitItem()                                  // initial
        viewModel.onEvent(WelcomeEvent.Refresh)
        assertTrue(awaitItem().isRefreshing)
        val settled = awaitItem()
        assertFalse(settled.isRefreshing)
        assertNotNull(settled.message)
    }
}
```

Turbine (`.test { }`) reads a `Flow` item by item and fails the test if an expected item never
arrives, so there is no polling and no sleeping.

### Fakes, not mocks

One fake per interface, shared from `:core:testing`:

```kotlin
class FakeGreetingRepository : GreetingRepository { … }
```

A mock asserts *how* the code is written — which methods it called, in what order — so it fails
when you refactor something that still works. A fake asserts what the code *does*. Fakes also give
a test real behaviour to work against (`emit(...)`, `failNextRefresh()`), which makes error paths
easy to reach. A hand-written fake is a few more lines once, and fewer lines in every test after.

### Coroutines and time

- `runTest` for anything `suspend`. Never `runBlocking` outside a test, never `Thread.sleep`.
- Inject `TestAppDispatchers` so background work runs on the test scheduler and completes
  deterministically.
- Inject the clock. `OfflineFirstGreetingRepository` takes `now: () -> Long`, so a test passes
  `{ 1_000L }` and asserts an exact timestamp.
- `Dispatchers.setMain(...)` in `@BeforeTest` and `Dispatchers.resetMain()` in `@AfterTest` for
  anything using `viewModelScope`.

## 3. Architecture tests

`:architecture-tests` turns [ARCHITECTURE.md](ARCHITECTURE.md) into Konsist assertions — feature
isolation, domain purity, data-source access, ViewModel purity, `expect`/`actual` confinement. They
run in `qualityCheck`.

When you break one, the fix is almost always to move the code, not to widen the rule. If a rule is
genuinely wrong, change the rule and this document in the same pull request, with the reason.

## 4. End-to-end tests (Appium)

Black-box tests that drive the installed app the way a person does. They live in `tools/e2e`,
which is a small pytest suite with page objects.

```
tools/e2e/
├── conftest.py          the Appium driver, per platform
├── pages/               one page object per screen
├── tests/               the scenarios
├── requirements.txt
└── run.sh               starts Appium, runs pytest, stops Appium
```

### Running them

```bash
# Android: build and install first, then
tools/e2e/run.sh

# a single scenario
tools/e2e/run.sh tests/test_welcome.py::test_theme_choice_survives_restart

# iOS (needs a built .app and the XCUITest driver)
E2E_PLATFORM=ios E2E_APP="$PWD/build/ios/Debug-iphonesimulator/iosApp.app" tools/e2e/run.sh
```

`run.sh` installs nothing globally except Appium's drivers; the Python dependencies go into
`tools/e2e/.venv`.

### The rules that keep them from rotting

- **Find elements by `TestTags`, never by coordinates or by translated text.** Coordinates break on
  every layout change; visible text breaks the moment someone runs the suite in Spanish.
  `TestTags` is shared Kotlin, so a rename is a compile error in the app and a one-line change in
  the page object.
- **Page objects hold the locators; tests hold the intent.** A test reads
  `welcome.open_settings()`, not `driver.find_element(...)`. A screen change touches one page
  object, not twenty tests.
- **A change to a screen updates its page object in the same pull request.** A new user flow gets a
  scenario.
- **No sleeps.** Use the explicit waits in `pages/base.py`; they scale up on CI, which is far
  slower than a laptop.
- **Deterministic data.** A scenario sets up what it needs and does not depend on files that happen
  to exist on one machine.

### How tags reach Appium

`Modifier.exposeTestTagsToAutomation()` (a seam in `:core:platform`) publishes the tags: on Android
it sets `testTagsAsResourceId`, so `Modifier.testTag("x")` becomes the resource id `x`; on iOS
Compose Multiplatform already exposes the tag as the accessibility identifier, so it does nothing.

**Apply it once per window, not once per app.** A `Dialog`, a `Popup` or a dropdown renders in its
own window, outside the root's semantics tree, and its tags are not published unless that window
opts in too. The symptom is confusing: the element is plainly on screen and the automation cannot
find it. `AboutDialog` in `App.kt` shows the fix.

On Android, Appium then finds a tag with `UiSelector().resourceId("x")` — **not** `AppiumBy.ID`,
which expands a bare id to `<package>:id/x` and so never matches. On iOS it is
`AppiumBy.ACCESSIBILITY_ID`. `pages/base.py` picks the strategy from `E2E_PLATFORM`, so a page
object is written once.

## 5. Coverage

Kover measures it and `qualityCheck` enforces the floor in the root build file. The floor exists to
stop coverage sliding, not to hit a number: a change that adds logic and no tests should fail.

Raise the floor as the app grows. Never lower it to make a build pass — add the test.

## 6. Before you say it is done

```bash
./gradlew qualityCheck
./gradlew allTests
./gradlew :androidApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
tools/e2e/run.sh          # for anything that changes a screen or a flow
```

Then run both apps and look at the screen. "The tests pass" is not the same as "it works".
