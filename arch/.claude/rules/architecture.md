---
paths:
  - "core/**"
  - "feature/**"
  - "composeApp/**"
  - "androidApp/**"
  - "settings.gradle.kts"
---

# Architecture

Loaded whenever you touch module code. The reasoning behind every line here, and the diagrams,
are in `docs/ARCHITECTURE.md`, which also holds the generated module graph — read it before
changing a layer or adding a seam.

### Layers, and the direction of dependencies

```
UI (Compose, ViewModels)  ->  Domain (use cases, optional)  ->  Data (repositories)  ->  data sources
```

Dependencies point one way only. A lower layer never imports a higher one.

- **UI layer** (`:feature:*`): screens and state holders. A screen is a function of its state.
- **Domain layer** (`:core:domain`): use cases. **Optional.** Add one only when logic is reused by
  two or more ViewModels, combines two or more repositories, or is complex enough to deserve its
  own tests. A use case that forwards a single repository call is noise — delete it.
- **Data layer** (`:core:data`): repositories own the business logic and are the single entry point
  for each kind of data. Nothing above them may touch a DAO, an HTTP client or DataStore.
- **Data sources** (`:core:database`, `:core:network`, `:core:datastore`): each wraps exactly one
  source and is reachable only through a repository.

### Unidirectional data flow

State flows **down** as immutable values; events flow **up** as method calls.

- A ViewModel exposes one `uiState: StateFlow<SomeUiState>`, read-only.
- The UI calls `onEvent(...)`. It never writes state.
- **The ViewModel never sends events to the UI.** No `Channel`, no `SharedFlow` of one-shot events:
  they are lost when the UI is stopped. Put the outcome in the state and clear it once shown
  (see `WelcomeUiState.message`).

### Single source of truth

The local database owns the data. The network writes into it; the UI reads from it. A refresh
therefore reaches the screen through the database's `Flow`, never as a return value
(`OfflineFirstGreetingRepository`).

### Work that must outlive a screen

`viewModelScope` ends when the user leaves the screen, and everything launched in it is cancelled.
A download, a sync or a write the user expects to finish anyway (switching language and leaving
Settings, for example) runs in an **application-wide scope owned by the repository**:
`externalScope.async { … }.await()`, with the scope (`SupervisorJob()` + the IO dispatcher)
injected from DI so a test can pass its own. The caller still awaits the result; leaving only
stops the waiting, not the work. A test proves it: start the work, cancel the caller, and check
the work still completed. (An app built from this template lost a language's audio download this
way: the sync ran in the Settings screen's scope.)

### Modules

| Module | Holds | May depend on | Must never depend on |
|---|---|---|---|
| `:androidApp` | `Application`, one `Activity`, manifest | `:composeApp` | anything else |
| `iosApp/` | SwiftUI shell (Xcode) | the `ComposeApp` framework | — |
| `:composeApp` | `App()`, navigation, DI wiring, iOS framework | every module below | — |
| `:feature:*` | screens, `UiState`, events, ViewModels | `:core:domain`, `:core:designsystem`, `:core:ui`, `:core:model`, `:core:platform` | **another feature**, any data-source module |
| `:core:domain` | use cases | `:core:data`, `:core:model` | Compose, Android, platform code |
| `:core:data` | repositories, mappers | data-source modules, `:core:model`, `:core:common` | Compose, any feature |
| `:core:database` `:core:network` `:core:datastore` | one data source each | `:core:model`, `:core:common`, `:core:platform` | `:core:data`, each other, features |
| `:core:platform` | every `expect`/`actual` | `:core:common` | anything else |
| `:core:designsystem` | theme, tokens, reusable widgets | `:core:model` | ViewModels, data, features |
| `:core:ui` | `UiText`, `TestTags` | `:core:model` | ViewModels, data, features |
| `:core:model` `:core:common` | models; dispatchers, `AppResult` | nothing in this project | everything |
| `:core:testing` | fakes, test helpers | `:core:data`, `:core:domain`, `:core:model` | used only via `commonTest` |

**Split by feature, not by layer.** A feature module rebuilds alone; a `:presentation` module
rebuilds on every change anywhere in the UI.

**When to add a module.** Add one when code is shared by two or more features, or when a feature is
large enough to own its build. Do not create a module for a single class.

`:architecture-tests` enforces the table above with Konsist. A violation fails the build.

### Platform code

Every `expect`/`actual` lives in `:core:platform`, behind an interface. One documented exception:
`:core:database`, because Room's own API differs per platform — its builder needs a `Context` on
Android and not on iOS, and its KSP compiler generates the `actual`s for `@ConstructedBy`, so there
is no hand-written code to move.

`LayerDependencyTest` allows `expect` in `:core:platform` and `:core:database` and nowhere else. It
matches on the module path rather than on a list of file names, so the rule means the same thing in
every project that adopts it. A seam anywhere else needs the same justification, in writing, in
`.claude/project.md`.

### Kotlin style

- Prefer lambdas and higher-order functions to single-method interfaces and anonymous classes.
- `sealed interface` for states and events, `data class` for models, `enum` for fixed choices.
- Coroutines and `Flow` between layers. Never `GlobalScope`, never `runBlocking` outside tests.
  Inject `AppDispatchers`; never reach for `Dispatchers.IO` inside a class.
- `CancellationException` is rethrown, never swallowed (`runCatchingApp`).
- No `!!`. Handle null at the boundary.
- One implementation per concept. Before writing a helper, look for the existing one.
