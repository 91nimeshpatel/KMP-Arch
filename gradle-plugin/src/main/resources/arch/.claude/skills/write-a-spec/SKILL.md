---
name: write-a-spec
description: Turn a committed intent into a requirements and design spec, flagging anything that conflicts with the project's rules. Use when an intent has been accepted and someone asks what it would take to build, or asks for a spec.
---

# Writing a spec from an intent

The spec says **what will be built and where it conflicts with what already exists**. You write it;
the person who raised the intent reviews it. They may not be an engineer, so write it for them.

## 1. Read the intent, then read the rules

Open the intent in `docs/intents/`. Then read what the project already decided, because the point of
writing the spec now is to hit the conflicts now rather than in review weeks later:

- `CLAUDE.md` and the files in `.claude/rules/` — the binding rules
- `docs/ARCHITECTURE.md` — the layers, the seams, and the decisions already recorded
- `docs/LIBRARIES.md` — what each dependency can and cannot do

## 2. Find the conflicts first

This is the most valuable part of the document, so do it before drafting anything else. Look for:

- **Rule conflicts** — the ask needs something the architecture forbids, such as a feature reaching
  another feature, or a screen touching the database directly.
- **Platform limits** — something one platform can do and the other cannot. Say so plainly; the
  answer is rarely "do it on Android only", but it is always better known now.
- **Library limits** — check `docs/LIBRARIES.md` before assuming a dependency can do it.
- **Two rules that disagree with each other.** Name both and recommend one.

For each: what the conflict is, the options, your recommendation. **Never quietly pick one and move
on** — that decision belongs to the person reviewing.

If you find none, say so explicitly, having looked.

## 3. Write the requirements so they can be checked

Each one should be something you could later point at and say "yes, it does that" — because these
become the tests. "The theme choice survives a restart" is a requirement. "Good performance" is a
wish.

## 4. Sketch the design, do not write the code

Which modules, which screens, where the data comes from, what differs per platform. Enough to start
from. If you are naming classes, you have gone too far — that is the plan's job.

## 5. Hand it back

Save as `docs/intents/<same-name>-spec.md`, next to its intent. Show it to whoever raised the intent
and walk them through the concerns first. They decide whether it proceeds.

Commit the pair together: the intent records what was asked for, the spec what was decided.

## After it is accepted

The spec is finished when the build starts. You may amend it before then; amending it afterwards
means the requirements moved during the build, which is worth noticing rather than hiding.

It is **not** a description of the current app and is never brought up to date later. What the app
does today is told by the code and, more reliably, by the tests.
