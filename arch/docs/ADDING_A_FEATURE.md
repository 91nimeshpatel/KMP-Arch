# Adding a feature

The recipe for a new screen. `feature/welcome` is the worked example — read it alongside this.

Everything here follows [ARCHITECTURE.md](ARCHITECTURE.md); if a step surprises you, that document
says why.

---

## 0. Decide whether you need a module

A new **module** is right when the feature is a user-facing area of the app that can stand alone.
Two screens that are two tabs of the same thing belong in one module.

Do **not** create a module for a single shared class — that goes in `:core:*`.

## 1. Register the module

`settings.gradle.kts`:

```kotlin
include(":feature:profile")
```

`feature/profile/build.gradle.kts` — the whole file:

```kotlin
plugins {
    id("kmp.feature")
}

kotlin {
    android { namespace = "com.example.app.feature.profile" }
    sourceSets {
        commonMain.dependencies {
            // only what this feature needs beyond what kmp.feature already provides
        }
    }
}
```

`kmp.feature` already brings Compose, `:core:designsystem`, `:core:ui`, `:core:model`,
`:core:domain`, `:core:platform`, lifecycle and Koin. **Never put a version in here.**

## 2. The state and the events

`ProfileUiState.kt`:

```kotlin
/** Everything the profile screen draws, in one immutable value. */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val message: UiText? = null,
)

/** Everything the profile screen can ask for. */
sealed interface ProfileEvent {
    data class NameChanged(val value: String) : ProfileEvent
    data object Save : ProfileEvent
    /** The UI has shown [ProfileUiState.message]; clear it. */
    data object MessageShown : ProfileEvent
}
```

One state class, not five `StateFlow`s: two flows can be observed in an inconsistent combination,
one value cannot. Messages are `UiText`, never `String` — the ViewModel has no locale.

## 3. The ViewModel

```kotlin
class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        repository.observeProfile()
            .map { ProfileUiState(isLoading = false, name = it.name) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ProfileUiState(),
            )

    fun onEvent(event: ProfileEvent) { … }

    private companion object {
        /** Keeps the stream alive across a rotation without keeping it alive in the background. */
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
```

- One read-only `StateFlow`, one `onEvent`. No `Channel` of one-shot events — the reason is in
  [ARCHITECTURE.md](ARCHITECTURE.md#two-principles-hold-it-together).
- `WhileSubscribed(5_000)`: five seconds outlives a configuration change and nothing else.
- No Android or Compose imports. `:architecture-tests` fails the build if one appears.

## 4. The screen

`ProfileScreen.kt` holds two composables:

```kotlin
/** Connects the screen to its ViewModel. The only place that knows DI exists. */
@Composable
fun ProfileRoute(viewModel: ProfileViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(state = state, onEvent = viewModel::onEvent)
}

/** The screen itself: a pure function of [state]. Previewable and testable with no dependencies. */
@Composable
fun ProfileScreen(state: ProfileUiState, onEvent: (ProfileEvent) -> Unit) { … }
```

While writing it:

- Read tokens, never literals: `AppTheme.spacing.medium`, `MaterialTheme.colorScheme.primary`.
  A `.dp` literal or a `Color(0x…)` in a feature is a defect.
- Reuse `:core:designsystem` components (`AppButton`, `AppCard`, `AppLoading`, `AppMessage`)
  before writing a new one. If you write one that another feature could want, it belongs there.
- `Modifier.testTag(TestTags.PROFILE_SCREEN)` on the root and on anything E2E touches. If the
  screen opens a dialog or a popup, add `Modifier.exposeTestTagsToAutomation()` to it as well —
  a new window does not inherit the root's tag publishing (docs/TESTING.md).
- `start`/`end`, never `left`/`right`.
- Hoist state. A composable that owns state it did not create cannot be tested.

## 5. The strings

`feature/profile/src/commonMain/composeResources/values/strings.xml`, **and every other locale in
the same change** (`values-es/`, `values-hi/`):

```xml
<resources>
    <string name="profile_title">Profile</string>
</resources>
```

Use them through `UiText.Resource(Res.string.profile_title)` from the ViewModel, or
`stringResource(Res.string.profile_title)` directly in the composable. A missing key is a crash at
runtime, not a fallback.

**Android needs one more line.** Add the module to `composeResourceModules` in
`androidApp/build.gradle.kts`; the comment there explains why that bridge exists. Forgetting it is a
crash on your new screen, not a build error.

## 6. The data, if the feature needs new data

Work upward from the source:

1. **Data source** — a DAO and entity in `:core:database`, an API and DTO in `:core:network`, or a
   key in `:core:datastore`. One source per module; they never call each other.
2. **Model** — a plain class in `:core:model`, no annotations from any framework.
3. **Repository** — an interface and an implementation in `:core:data`, plus a mapper. The
   repository is the only thing that touches the data sources, and it decides which source wins
   (see `OfflineFirstGreetingRepository`).
4. **Use case** — only if the logic is shared by two ViewModels, combines two repositories, or is
   complex enough to test on its own. Otherwise skip it.

A feature that reaches past its repository into a DAO fails `:architecture-tests`.

## 7. Wire it up

`composeApp/di/AppModules.kt`:

```kotlin
val dataModule = module {
    single<ProfileRepository> { DefaultProfileRepository(get(), get()) }
}

val featureModule = module {
    viewModel { ProfileViewModel(get()) }
}
```

`composeApp/navigation/AppNavigator.kt`, if it is a top-level destination:

```kotlin
data object Profile : TopLevelDestination("profile")
```

Add it to `fromKey`, to the destination list in `App.kt`, and to the `when` that picks the screen.
The `when` is exhaustive over a sealed class, so the compiler will not let you forget the screen.

## 8. Test it

`feature/profile/src/commonTest/.../ProfileViewModelTest.kt` — the initial state, each event, and
the failure path. Use the fakes in `:core:testing`; add one there if the interface is new.

If the feature adds a user journey, add an Appium scenario and a page object in `tools/e2e`.
[TESTING.md](TESTING.md) has the detail.

## 9. Check it

```bash
./gradlew qualityCheck
./gradlew allTests
./gradlew :androidApp:installDebug          # and run it
open iosApp/iosApp.xcodeproj                # and run it
```

Then read your own diff once, as a reviewer would. Anything you would question there, fix before
you open the pull request.

## Checklist

- [ ] module registered in `settings.gradle.kts`, build file has no versions
- [ ] `UiState` immutable, events sealed, one `StateFlow`, no one-shot channel
- [ ] ViewModel free of Android and Compose imports
- [ ] `Route` and `Screen` split; `Screen` is a pure function of its state
- [ ] tokens only — no colour, spacing or font-size literals
- [ ] test tags on the root and on anything E2E uses
- [ ] strings in every locale, `UiText` across the ViewModel boundary
- [ ] data reached only through a repository
- [ ] registered in Koin and in the navigator
- [ ] tests for every state transition; E2E scenario if it is a new journey
- [ ] KDoc on every public declaration
- [ ] ran on Android **and** iOS
