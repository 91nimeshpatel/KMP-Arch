# This project

Everything specific to this project lives here. The plugin writes this file once, when it is first
adopted, and never touches it again — so it is safe to put anything in it.

Use it for what the shared rules cannot know:

- what this app is, and who uses it
- decisions that differ from the shared standards, **and why**
- anything a new person, or a new agent, would otherwise have to work out from the code

## Where the shared rules disagree with this project

If a shared rule does not fit, record the exception here rather than editing the file it came from —
an edited upstream file is overwritten on the next update, and the reason for the change is lost
with it.

Write down the reason, not just the exception. "We use SQLDelight, not Room, because this app also
targets the web" survives; "we use SQLDelight" does not.

If the same exception turns up in a second project, the shared rule is wrong. Fix it there.
