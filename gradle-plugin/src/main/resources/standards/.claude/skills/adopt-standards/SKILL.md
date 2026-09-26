---
name: adopt-standards
description: Install or update the shared engineering files in this project — CLAUDE.md, the path-scoped rules, the CI and release workflows, CODEOWNERS and the PR template. Use when setting up a new project from the KMP template, when asked to adopt or sync the standards, or when the plugin has been updated and the project should pick up the changes.
---

# Adopting the shared standards

A Claude Code plugin can carry skills, agents and hooks, but not `CLAUDE.md`, `.claude/rules/` or
`.github/workflows/` — those are read by Claude Code and by GitHub, not by the plugin system. So the
plugin **bundles** them and this skill copies them in.

The bundled files live at `${CLAUDE_PLUGIN_ROOT}/project/`. Use that path; do not guess at it.

## Who this is for

Projects built from **KMP-Template**. The bundled rules describe `:core:*` and `:feature:*` modules
and a Compose Multiplatform build. In a project laid out differently they would be wrong, so install
them there only deliberately, and say so.

## What is owned by whom

| Owner | Files | Rule |
|---|---|---|
| **The plugin** | `CLAUDE.md`, `.claude/rules/**`, `.github/**`, `REVIEW.md`, `architecture-tests/**`, `tools/**` | replaced on every update — never edit them in the project |
| **The project** | `.claude/project.md`, everything else | written once if missing, then never touched again |

A project that needs a shared file to be different does **not** edit it. It records the exception in
`.claude/project.md`, with the reason. An edited upstream file is overwritten on the next update and
the reason is lost with it.

## Do this

### 1. Say what will change, before changing it

List every bundled file and compare it with the project:

- **missing** → will be created
- **identical** → nothing to do
- **different** → will be replaced, and the project's version is about to be lost

Show that list and **stop**. Do not write anything until the person has seen it. If a file is
different, show the diff — they may have edited it deliberately, and that is worth knowing before
it disappears.

### 2. Respect an opt-out

If `.standards-ignore` exists in the project root, skip every path it lists (one glob per line,
`#` for comments). Report what was skipped, so an opt-out nobody remembers cannot hide.

### 3. Copy

Copy each file from `${CLAUDE_PLUGIN_ROOT}/project/` to the same relative path in the project,
preserving directories. Create `.claude/project.md` **only if it does not exist** — it belongs to the
project, and overwriting it would delete exactly the context this skill exists to preserve.

### 4. Check the build wiring for `architecture-tests`

The rules only run if the build knows about them. A project generated from KMP-Template already has
this; one adopting the standards for the first time may not. Check, and say which is missing:

- `settings.gradle.kts` includes `":architecture-tests"`
- the root `qualityCheck` task depends on `":architecture-tests:test"`

Do not add them silently — changing someone's build is a bigger step than copying a file, and it
should be their decision.

### 5. Tell them what to do next

- `.github/CODEOWNERS` ships with a placeholder handle. It must be changed, or reviews go nowhere.
- Branch protection is not applied by copying a file. It needs
  `tools/apply-branch-protection.sh`, and GitHub refuses it on a private repository on the Free
  plan.
- `CLAUDE.md` now ends with `@.claude/project.md`. Anything specific to this project goes there.

### 6. Verify, do not assume

Run the project's own gate afterwards — `./gradlew qualityCheck` — because a rules file that
arrives mid-change can fail the build, and it is better to find that now than in CI.

## When the plugin is updated

Run this skill again. Same comparison, same list, same stop before writing. Files the project owns
are untouched; files the plugin owns come up to date.
