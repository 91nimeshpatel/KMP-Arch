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

### `adopt-standards` — the files a plugin cannot carry

A plugin can hold skills, agents and hooks. It **cannot** hold `CLAUDE.md`, `.claude/rules/` or
`.github/workflows/` — those are read by Claude Code and by GitHub, not by the plugin system. So the
plugin bundles them under `kmp/project/` and the `adopt-standards` skill installs them:

| Installed | What it is |
|---|---|
| `CLAUDE.md` | the shared contract, ending in `@.claude/project.md` |
| `.claude/rules/**` | five path-scoped rules that load when you open a matching file |
| `architecture-tests/**` | the executable rules — layering, locale parity, documented dependencies and seams |
| `.github/workflows/**` | `ci`, `release`, `release-cut`, `hotfix` |
| `.github/CODEOWNERS`, PR template, branch ruleset | review and merge policy |
| `tools/` | **`new-feature.py`** — scaffolds a feature module, wired up and passing the gate; plus the module-graph generator and the branch-protection script |
| `REVIEW.md` | what a review looks for |

It lists what it will change and **stops before writing**, honours a `.standards-ignore`, and writes
`.claude/project.md` only if it is missing.

### Who owns what

| Owner | Files | Rule |
|---|---|---|
| The plugin | everything in the table above | replaced on update — never edit them in a project |
| The project | `.claude/project.md`, and everything else | never touched |

A project that needs a shared file to be different records the exception in `.claude/project.md`
with the reason, rather than editing the file. An edited upstream file is overwritten on the next
update and the reason goes with it. **The same exception in two projects means the shared file is
wrong** — fix it here.

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

**Anything unproven against a second project.** The build conventions and the architecture rules are
still in [KMP-Template](https://github.com/mrlonewolfer/KMP-Template). They move here when a second
project has shown what they need to do for two consumers rather than one.

## Keeping the bundle honest

The files under `kmp/project/` are copies of files that live, and are actually exercised by a build,
in [KMP-Template](https://github.com/mrlonewolfer/KMP-Template). A copy drifts — this one was stale
within an hour of being made — so it is refreshed by a script rather than by hand:

```bash
tools/sync-from-template.sh              # refresh from ../KMP Template
tools/sync-from-template.sh --check      # fail if anything is stale
```

CI runs `--check` against a fresh checkout of the template — **when it can reach it.** The template
is a private repository and this one's `GITHUB_TOKEN` does not reach it, so that step is currently
skipped with a warning rather than failing. To turn it on, either make the template public or add a
`TEMPLATE_READ_TOKEN` secret with read access to it. Until then, run `--check` before a release.

## Versioning

`kmp/.claude-plugin/plugin.json` pins the version, and Claude Code keeps everyone on the cached copy
until that string changes. **A fix without a version bump reaches nobody** — that is not a figure of
speech; it happened here, and `claude plugin update` reported "already at the latest version" while
serving the broken copy. A release is a sync, a version bump and a push.

## Related

[KMP-Template](https://github.com/mrlonewolfer/KMP-Template) — the project these standards came
from, and the worked example they describe.
