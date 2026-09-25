# KMP Standards

Engineering standards for Kotlin Multiplatform projects, packaged so that every project gets the
same ones from one place instead of each keeping its own copy that slowly drifts.

Today this holds one Claude Code plugin. The build conventions, the architecture rules and the
reusable CI workflows are intended to follow, once there is a second project to prove them against.

## Install

```bash
claude plugin marketplace add mrlonewolfer/KMP-Standards
claude plugin install kmp@kmp-standards
```

Or, while working on the plugin itself:

```bash
claude --plugin-dir "/path/to/KMP Standards/kmp"
```

To turn it on for everyone who clones a repository, add it to that repository's committed
`.claude/settings.json`:

```json
{ "enabledPlugins": { "kmp@kmp-standards": true } }
```

## What the `kmp` plugin gives you

### Skills — loaded when the work matches

| Skill | Use it when |
|---|---|
| `add-a-feature` | adding a screen, module, ViewModel or user-visible string |
| `change-a-dependency` | adding, upgrading or removing any library, plugin or version |
| `capture-intent` | someone describes a want or a problem and no `intent.md` exists yet |
| `write-a-spec` | an intent is accepted and someone asks what it would take to build |

### Subagent

`verifier` builds, installs and runs the app on Android and iOS, exercises the changed behaviour and
reports what it actually saw. Its verdict is worth having precisely because it did not write the
code.

### Hooks — enforcement, not advice

| Hook | Fires |
|---|---|
| `catalog-changed` | after an edit to `gradle/libs.versions.toml`, naming versions missing from `docs/LIBRARIES.md` |
| `docs-gate` | when a turn tries to finish while a document contradicts the code |

Both are quiet in a project that does not keep those files, so the plugin can be installed anywhere
without lecturing.

## What is deliberately not here

**Project-specific content.** A project's `docs/ARCHITECTURE.md` and `docs/LIBRARIES.md` describe
*its* modules and *its* dependencies — shipping one project's copy to another would be worse than
shipping nothing, because it would be confidently wrong. What ships instead is the *shape*: the
recipe in `kmp/reference/ADDING_A_FEATURE.md`, and the entry format in
`kmp/reference/library-entry-format.md`.

**Path-scoped rules.** The `.claude/rules/` files a project keeps are not a plugin component, so
they stay with each project for now.

**Anything unproven against a second project.** The build conventions and the architecture rules are
still in [KMP-Template](https://github.com/mrlonewolfer/KMP-Template). They move here when a second
project has shown what they need to do for two consumers rather than one.

## Versioning

`kmp/.claude-plugin/plugin.json` pins the version. Claude Code keeps everyone on the cached copy
until that string changes, so a release is a version bump and a tag.

## Related

[KMP-Template](https://github.com/mrlonewolfer/KMP-Template) — the project these standards came
from, and the worked example they describe.
