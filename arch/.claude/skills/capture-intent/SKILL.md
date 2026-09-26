---
name: capture-intent
description: Turn an idea, a complaint or a bug report into a committed intent.md before any code is written. Use when someone describes something they want, a problem they have, or a feature to build, and no intent exists yet.
---

# Capturing an intent

Before a line of code, write down **what is wanted, why, and under what constraints**. The reason
for a change is the first thing lost and the hardest to reconstruct.

## 1. Interview first, write second

Do not start drafting. Ask the questions an analyst would, one or two at a time, until the idea is
concrete:

- What can you not do today? What happens instead?
- Who is affected, and how often? What evidence is there — a number, a complaint, a support volume?
- What does better look like, from their point of view?
- What must this respect? Platforms, privacy, an existing contract, a store policy, a deadline.
- What is explicitly **out** of scope?
- What is still genuinely undecided?

**Push back on solutions offered as problems.** If someone says "add a status endpoint", ask what
the person on the other end is trying to find out. A well-stated problem often has a cheaper answer
than the one first imagined.

## 2. Write it up

Use `docs/intents/TEMPLATE.md`, saved as `docs/intents/<short-name>.md`. Keep the originator's own
words where you can — a rewrite in neutral corporate prose loses the detail that made it useful.

State the problem, not the design. If you find yourself naming a class or a library, you have gone
too far; that belongs in the plan.

## 3. Hand it back

Show the draft to whoever described it and let them correct it. They own the wording; you own the
structure. Do not commit an intent the originator has not read.

## 4. Commit it

One file, on its own, with a `docs:` commit. The author and timestamp become the record.

Then stop. An intent is not a licence to start building — it is what the decision to build gets made
from. If the next step is obvious and small, say so and ask.
