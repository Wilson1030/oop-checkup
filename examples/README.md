# Demo code

`before/` and `after/` are two versions of the same library-management system,
used to demonstrate the tool's output.

| | |
|---|---|
| `before/` | Deliberately written as "C in Java": global `static` state, anemic entity classes, `public` bare fields, `switch`-based type dispatch, all logic in `main` |
| `after/` | Same functionality, refactored along object-oriented lines |

Both compile and run:

```bash
javac -encoding UTF-8 -d out before/library/*.java
javac -encoding UTF-8 -d out after/library/*.java
```

Compare them:

```bash
../checkup.sh before
../checkup.sh after
```

## ⚠️ These are not validation samples

The code in this directory exists **to demonstrate output**.

Accuracy validation uses **third-party code only** — 2 mature open-source libraries
and 11 real student repositories (see `../runs/`). Validating a tool against samples
written by its own author is circular reasoning, and that line is not crossed here.
