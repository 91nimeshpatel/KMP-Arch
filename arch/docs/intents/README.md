# Intents

Every feature and every release starts as an `intent.md` here: what is wanted, why, and under what
constraints — written before anyone opens an editor.

An intent is deliberately **not** a design. It is the problem in the originator's own words, in a
form both a person and an agent can act on. It exists so that the reason for a change survives the
change: six months later the code says what was built, the commit says what changed, and only the
intent says *why anyone wanted it*.

## How it works

1. Describe the problem in your own words — to Claude, or straight into a file. No formal language.
   You do not need to know how it should be built; that is the next stage's job.
2. Write it up as `docs/intents/<short-name>.md` using [`TEMPLATE.md`](TEMPLATE.md). The
   `capture-intent` skill does this for you and asks the questions an analyst would.
3. Correct anything that was misunderstood. The originator owns the wording.
4. Commit it. The author and timestamp become part of the record, and the Git history of this
   folder is the audit trail: who asked for what, and when.

## What makes a good one

- **State the problem, not the solution.** "Customers phone to ask where their claim is" beats
  "add a status endpoint". A well-stated problem often has a cheaper solution than the one first
  imagined.
- **Say who is affected.** A change nobody can name a user for is a change worth questioning.
- **Write the constraints down.** They are the part that gets forgotten and then violated.
- **Keep the open questions.** An intent with unanswered questions is honest; one with none is
  usually incomplete.

## The spec that follows it

Once an intent is accepted, Claude turns it into `<name>-spec.md` beside it, using
[`TEMPLATE-spec.md`](TEMPLATE-spec.md) and the `write-a-spec` skill. You review it; you do not write
it, and you do not need to be an engineer to review it.

The spec answers **what will be built, and where it conflicts with what already exists**. That second
half is the point. Claude reads the project's own rules while writing, so a clash between the ask and
the architecture surfaces now rather than in a review weeks later. Those conflicts go at the top of
the document, because they are the decisions only a person can make.

The two files are committed together. The intent records what was asked for; the spec records what
was decided.

### They are not kept up to date

A spec is finished when the build starts. Amend it before then if the requirements change; amending
it afterwards means they moved mid-build, which is worth noticing rather than hiding.

Neither file is ever brought up to date afterwards, and that is deliberate — it is what stops them
becoming a pile of documents that quietly contradict the app. **What the code does today is told by
the code, and more reliably by the tests.** These two say what was wanted and what was agreed, on
that date.

## What happens next

The intent is reviewed, then becomes a plan and a change. When the work lands, the pull request
links back here. An intent that is rejected stays in the history with the reason — knowing what was
considered and declined is as useful as knowing what was built.
