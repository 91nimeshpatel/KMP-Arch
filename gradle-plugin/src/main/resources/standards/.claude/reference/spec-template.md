# Spec: <short title>

Pairs with: `<name>.md` · Author: Claude, reviewed by <name> · Date: <YYYY-MM-DD> ·
Status: draft | accepted | declined

> Written by Claude from the intent, then reviewed by whoever raised it. You do not need to be an
> engineer to review this — if a section does not make sense to you, that is a problem with the
> section.

## Concerns and conflicts

**Read this first.** Anything that cannot be done as asked, or where two of the project's rules
disagree, or where a platform will not allow it. These are the points an analyst would have
escalated, and they are cheaper to settle now than after the code exists.

For each one: what the conflict is, the options, and a recommendation. Say "no good option" if that
is the truth.

*Write "None" if there genuinely are none — but look properly first.*

## The ask

One or two paragraphs restating the problem in your own words, so this document stands on its own.
If this does not match the intent, the intent wins and this is wrong.

## Requirements

Numbered, and each one testable — something you could later point at and say "yes, it does that".

1. …
2. …

Not a requirement: "fast", "clean", "modern". If nobody can tell whether it is met, it is a wish.

## Design outline

Which modules change, which screens, where the data comes from. Enough for someone to start, not so
much that it becomes the code.

- **New or changed modules**:
- **Screens**:
- **Data**: what is stored, what is fetched, what is derived
- **Platform differences**: anything Android and iOS cannot share, and why

## Rules this change touches

Which of the project's existing rules apply, and confirmation the design respects them — or the
conflict named above. See `CLAUDE.md`, `.claude/rules/` and `docs/`.

## Out of scope

What this deliberately does not do, so the work does not quietly grow.

## Open questions

Carried forward from the intent, plus any new ones. An unanswered question is cheaper than a wrong
assumption.
