#!/usr/bin/env python3
"""Create a feature module, wired up and ready to build.

Like the New Project wizard, but for this architecture: it writes the module, a screen split into a
route and a pure screen, a ViewModel with one read-only StateFlow, a sealed event type, a string
resource in every locale, a test, and the three registrations a new module needs.

    python3 tools/new-feature.py --name profile
    python3 tools/new-feature.py --name profile --dry-run

The third registration is the one worth having a script for: a module with composeResources that is
missing from `composeResourceModules` in androidApp/build.gradle.kts crashes at runtime, on the
screen that uses it, with no build error. It is the trap this file exists to close.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def package_of() -> str:
    """The project's package, read from gradle.properties rather than assumed."""
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(r"^template\.applicationId=(.+)$", text, re.M)
    if not match:
        sys.exit("Could not read template.applicationId from gradle.properties")
    return match.group(1).strip()


def locales() -> list[str]:
    """Every locale the project already ships, so a new string is never missing from one."""
    found = {d.name for d in (ROOT / "feature").glob("*/src/commonMain/composeResources/values-*")}
    return ["values"] + sorted(found)


def files(name: str, pkg: str) -> dict[str, str]:
    cls = name[0].upper() + name[1:]
    fpkg = f"{pkg}.feature.{name}"
    path = f"feature/{name}/src/commonMain/kotlin/{fpkg.replace('.', '/')}"

    out = {
        f"feature/{name}/build.gradle.kts": f'''plugins {{ id("template.kmp.feature") }}

kotlin {{
    android {{
        namespace = "{fpkg}"
    }}
}}

compose.resources {{
    publicResClass = true
    packageOfResClass = "{fpkg}.resources"
}}
''',
        f"{path}/{cls}UiState.kt": f'''package {fpkg}

/**
 * Everything the {name} screen shows.
 *
 * One immutable value, so the screen is a function of it and nothing else. Add what the screen
 * needs; never add anything only the ViewModel cares about.
 *
 * @property isLoading whether to show progress. Replace this with what the screen actually shows.
 */
data class {cls}UiState(
    val isLoading: Boolean = false,
)

/**
 * Everything the user can do on the {name} screen.
 *
 * A sealed interface, so adding a case makes the compiler find every place that has to handle it.
 */
sealed interface {cls}Event {{
    /** The screen became visible. */
    data object Shown : {cls}Event
}}
''',
        f"{path}/{cls}ViewModel.kt": f'''package {fpkg}

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State holder for the {name} screen.
 *
 * Knows nothing about Android or Compose — that is what lets it be tested on every platform the app
 * ships to. Inject what it needs; never reach for a singleton.
 */
class {cls}ViewModel : ViewModel() {{
    private val _uiState = MutableStateFlow({cls}UiState())

    /** Read-only, because the screen reports events and never writes state. */
    val uiState: StateFlow<{cls}UiState> = _uiState.asStateFlow()

    /** Handles a user action. */
    fun onEvent(event: {cls}Event) {{
        when (event) {{
            {cls}Event.Shown -> Unit
        }}
    }}
}}
''',
        f"{path}/{cls}Screen.kt": f'''package {fpkg}

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import {pkg}.core.designsystem.theme.AppTheme
import {fpkg}.resources.Res
import {fpkg}.resources.{name}_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The {name} screen, with its state holder attached.
 *
 * Split from [{cls}Screen] so the screen itself stays a pure function of its state: previewable,
 * and testable without a ViewModel.
 */
@Composable
fun {cls}Route(viewModel: {cls}ViewModel = koinViewModel()) {{
    // collectAsStateWithLifecycle, never collectAsState: the latter keeps collecting while the app
    // is in the background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    {cls}Screen(uiState = uiState, onEvent = viewModel::onEvent)
}}

/**
 * The {name} screen.
 *
 * @param uiState everything to show.
 * @param onEvent where user actions go.
 */
@Composable
fun {cls}Screen(
    uiState: {cls}UiState,
    onEvent: ({cls}Event) -> Unit,
    modifier: Modifier = Modifier,
) {{
    // Reported once when the screen appears. The ViewModel decides what that means; the screen
    // only says what happened.
    LaunchedEffect(Unit) {{ onEvent({cls}Event.Shown) }}

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(AppTheme.spacing.large)
                .testTag("{name}_screen"),
    ) {{
        Text(
            text = stringResource(Res.string.{name}_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        if (uiState.isLoading) {{
            CircularProgressIndicator()
        }}
    }}
}}
''',
        f"feature/{name}/src/commonTest/kotlin/{fpkg.replace('.', '/')}/{cls}ViewModelTest.kt": f'''package {fpkg}

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * What the {name} screen is required to do.
 *
 * These names are the specification: someone should be able to read the list and know what the
 * screen does without opening it.
 */
class {cls}ViewModelTest {{
    @Test
    fun `starts idle`() {{
        assertFalse({cls}ViewModel().uiState.value.isLoading)
    }}
}}
''',
    }

    # A screen test drives the real composable, which is what a ViewModel test structurally cannot
    # do: prove the state reaches the screen and that the tags the E2E suite relies on exist.
    out[f"feature/{name}/src/commonTest/kotlin/{fpkg.replace('.', '/')}/{cls}ScreenSpec.kt"] = f'''package {fpkg}

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Screen-level tests for [{cls}Screen].
 *
 * The screen is stateless, so a test hands it a [{cls}UiState] and a lambda — no ViewModel, no
 * dependency injection, no device. That is the payoff of splitting `Route` from `Screen`.
 *
 * Abstract, and run by a per-platform subclass, so nothing runs twice and nothing runs without a
 * platform opting in. See the existing features for why only iOS has a runner today.
 */
@OptIn(ExperimentalTestApi::class)
abstract class {cls}ScreenSpec {{
    @Test
    fun `renders the state it is given`() =
        runComposeUiTest {{
            setContent {{ {cls}Screen(uiState = {cls}UiState(), onEvent = {{}}) }}

            // The tag the Appium suite looks for. Asserting it here means a rename breaks a fast
            // unit test instead of a slow emulator run.
            onNodeWithTag("{name}_screen").assertIsDisplayed()
        }}

    @Test
    fun `reports that it was shown`() =
        runComposeUiTest {{
            var shown = false
            setContent {{
                {cls}Screen(
                    uiState = {cls}UiState(),
                    onEvent = {{ if (it is {cls}Event.Shown) shown = true }},
                )
            }}
            waitForIdle()

            assertTrue(shown, "the screen should report Shown when it appears")
        }}
}}
'''

    for folder in locales():
        out[f"feature/{name}/src/commonMain/composeResources/{folder}/strings.xml"] = (
            '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            f'    <string name="{name}_title">{cls}</string>\n</resources>\n'
        )
    out[f"tools/e2e/pages/{name}.py"] = f'''"""The {name} screen."""

from __future__ import annotations

from .base import Page


class {cls}Page(Page):
    root_tag = "{name}_screen"
'''

    out[f"tools/e2e/tests/test_{name}.py"] = f'''"""End-to-end checks for the {name} screen.

These run against a real build on a real device. They find elements by test tag, never by
coordinates or translated text, so they survive a redesign and a new locale.
"""

from __future__ import annotations

from pages.{name} import {cls}Page


def test_the_{name}_screen_opens(driver):
    """The screen is reachable and draws.

    Replace this with a journey once the screen does something: reach it the way a person would,
    rather than asserting it exists.
    """
    {cls}Page(driver).assert_open()
'''

    return out


def register(name: str, pkg: str, dry: bool) -> list[str]:
    """The three places a new module has to be named, and what happens if it is not."""
    done = []
    edits = [
        ("settings.gradle.kts", f'include(":feature:{name}")\n',
         re.compile(r'^include\(":feature:[a-z]+"\)\n', re.M), "last"),
        ("composeApp/build.gradle.kts", f'            implementation(project(":feature:{name}"))\n',
         re.compile(r'^ *implementation\(project\(":feature:[a-z]+"\)\)\n', re.M), "last"),
        # Without this one the screen crashes at runtime on Android with MissingResourceException.
        ("androidApp/build.gradle.kts",
         f'        ":feature:{name}" to "{pkg}.feature.{name}.resources",\n',
         re.compile(r'^ *":feature:[a-z]+" to "[^"]+",\n', re.M), "last"),
    ]
    for rel, line, pattern, _ in edits:
        p = ROOT / rel
        s = p.read_text(encoding="utf-8")
        if line.strip() in s:
            done.append(f"  already registered in {rel}")
            continue
        matches = list(pattern.finditer(s))
        if not matches:
            done.append(f"  COULD NOT register in {rel} — add by hand: {line.strip()}")
            continue
        at = matches[-1].end()
        if not dry:
            p.write_text(s[:at] + line + s[at:], encoding="utf-8")
        done.append(f"  {'would register' if dry else 'registered'} in {rel}")
    return done


def main() -> int:
    ap = argparse.ArgumentParser(description="Create a feature module.")
    ap.add_argument("--name", required=True, help="lower-case, one word, e.g. profile")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    name = args.name
    if not re.fullmatch(r"[a-z][a-z0-9]*", name):
        sys.exit(f"'{name}' must be one lower-case word, e.g. profile")
    if (ROOT / "feature" / name).exists():
        sys.exit(f"feature/{name} already exists")

    pkg = package_of()
    written = files(name, pkg)

    for rel, body in written.items():
        print(f"  {'would write' if args.dry_run else 'wrote'} {rel}")
        if not args.dry_run:
            p = ROOT / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(body, encoding="utf-8")

    print()
    for line in register(name, pkg, args.dry_run):
        print(line)

    if args.dry_run:
        print("\nRe-run without --dry-run to create it.")
        return 0

    graph = ROOT / "tools" / "module-graph.py"
    if graph.is_file():
        import subprocess
        subprocess.run([sys.executable, str(graph)], cwd=ROOT, check=False, capture_output=True)
        print("\n  regenerated the module graph")

    cls = name[0].upper() + name[1:]
    print(f"""
Next:
  1. Add {cls}ViewModel to the Koin module in composeApp/di/AppModules.kt — Koin resolves at
     runtime, so a missing binding is a crash on this screen, not a compile error.
  2. Add a destination for it in the navigator, if it is a top-level screen.
  3. Translate {name}_title in every values-* folder. It is currently the English word everywhere.
  4. Add a per-platform runner for {cls}ScreenSpec next to the existing features' runners, or the
     screen tests will not execute anywhere.
  5. The E2E test only checks the screen draws. Make it a journey: reach the screen the way a
     person would.
  6. ./gradlew qualityCheck allTests
""")
    return 0


if __name__ == "__main__":
    sys.exit(main())
