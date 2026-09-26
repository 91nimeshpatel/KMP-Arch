# How to write a library entry

Every dependency gets an entry in your project's `docs/LIBRARIES.md`. This is the shape it takes and
what each part is for. The entries themselves belong to your project — they describe how *you* use a
library, so they cannot be shared between projects.

## The shape

```markdown
### <Family> — `<catalog-alias-prefix>`
**Version** 1.2.3 · **Used in** `:core:network` · **Why** one sentence on what it is here for.

**How we use it** — the parts of the library this project actually touches, and the parts it
deliberately does not. Someone reading this should be able to tell whether a new use is normal
or novel.

**Precautions**
- The things that fail silently, or fail far from their cause.
- The mistake someone already made, so nobody makes it twice.

**On upgrade** — what to run or look at when the version moves. Not "run the tests" — the specific
thing this library breaks.
```

## What makes an entry worth having

**Write about your use, never paste the README.** The upstream docs say what the library can do.
The entry says what this project does with it, which is a much smaller and more useful thing.

**Precautions are the point.** An entry with no precautions usually means nobody has been bitten
yet, not that the library is safe. When you lose a day to something, the entry is where that day
gets banked.

**"On upgrade" is a instruction, not a sentiment.** "Launch both apps and open a screen that reads
from the database" is useful. "Be careful" is not.

**Delete precautions that stop being true.** A warning about a bug fixed two versions ago sends the
next person the wrong way. Removing it is part of the upgrade, and the pull request should say so.

## When an entry changes

Adding, upgrading or removing a dependency updates its entry **in the same commit** — the version,
the precautions the new release affects, and the "On upgrade" line. A version in the catalog that
does not appear in the playbook means one of them is lying.
