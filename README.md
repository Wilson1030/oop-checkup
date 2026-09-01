# oop-checkup

**An OO transition checklist for Java learners coming from C.**

[简体中文](README.zh-CN.md)

Your code compiles. It runs. It gets full marks. So **nothing ever tells you that
what you wrote is not object-oriented yet** — the compiler doesn't care, the tests
pass, and the grader only looks at output.

This tool fills that feedback vacuum. It doesn't rewrite your code. It tells you
where you are still writing C in Java, and why that matters.

---

## See it in 30 seconds

Same library-management system, before and after refactoring:

| `examples/before` | `examples/after` |
|---|---|
| <pre>1. Data and behaviour ...... 2 violations<br>2. Loose parameters ....... 1 unconfirmed<br>3. Polymorphism ........... 1 violation<br>4. static not abused ...... 2 violations<br>5. Encapsulation .......... 1 violation<br>6. Entry point ............ 1 violation<br><br>5 items violated</pre> | <pre>1. Data and behaviour ...... pass<br>2. Loose parameters ....... 1 unconfirmed<br>3. Polymorphism ........... pass<br>4. static not abused ...... pass<br>5. Encapsulation .......... pass<br>6. Entry point ............ pass<br><br>0 items violated</pre> |

Both versions compile and run. Diff them yourself — that is the whole lesson.

---

## Quick start

Requires JDK 17+ (Maven only needed to build).

```bash
git clone https://github.com/<your-name>/oop-checkup.git
cd oop-checkup
mvn package

./checkup.sh examples/before            # macOS / Linux
.\checkup.bat examples\before           # Windows
```

The launcher handles console encoding for you.

| Option | |
|---|---|
| `--lang zh\|en` | report language (default `zh`) |
| `--detail N` | expand at most N findings per item (default 3) |
| `--include-tests` | include test directories |
| `--summary` | one-line summary |
| `--batch` | treat each subdirectory as a separate project |
| `--config <file>` | LLM config (see [below](#optional-llm-explanations)) |
| `--no-llm` | force built-in templates |

<details>
<summary>Calling <code>java</code> directly</summary>

```bash
java -jar target/oop-checkup.jar <path> --lang en > report.txt
```

Writing to a file sidesteps all console-encoding issues.

⚠️ In **PowerShell**, `-Dfile.encoding=UTF-8` must be quoted, otherwise it is split
at the dot and you get `Could not find or load main class .encoding=UTF-8`:

```powershell
java "-Dfile.encoding=UTF-8" -jar target\oop-checkup.jar <path>
```
</details>

---

## The checklist

Every item maps to a recognised standard **with a citable source**. You may disagree
with the tool — but you can go read the original.

| # | Item | Standard | Source | |
|---|---|---|---|---|
| 1 | Data and behaviour kept together | Anemic Domain Model | Fowler, 2003 | ✅ |
| 2 | Loose parameters avoided | Data Clump / Long Parameter List | Refactoring #3 #4 | ⚠️ unconfirmed |
| 3 | Polymorphism instead of type checks | Switch Statements | Refactoring #11 | ✅ |
| 4 | `static` not abused | Global mutable state | standard teaching material | ✅ |
| 5 | Encapsulation intact | public mutable fields | Refactoring #5 (related) | ✅ |
| 6 | Entry point not overloaded | Long Method (main) | Refactoring #6 | ✅ |
| 7 | Primitive obsession avoided | Primitive Obsession | Refactoring #2 | ⬜ not implemented |

---

## What a finding looks like

```
────────────────────────────────────────────────────────────
  [MAJOR] Item 1 · Data and behaviour kept together
        Standard violated: Anemic Domain Model
        Source: Martin Fowler, AnemicDomainModel, 2003
────────────────────────────────────────────────────────────

  Book  —  5 fields, 6 getters/setters, 0 business methods

      Book.java:7   class Book
          LibraryService  accesses it 16 times
          Main            accesses it 4 times
          SearchService   accesses it 3 times

    > How you would have written it in C
      This is exactly the C shape: a struct holds the data, and a set of
      functions outside operates on it.
      You renamed the struct to a class and moved those functions into
      another class — but the structure never changed. Data on one side,
      the code that manipulates it on the other.

      In C you had no choice; the language only gave you struct.
      In Java you do have a choice. You just didn't take it.

    > Try this
      Find one method that only touches Book's own fields and move it into Book.
      Once you do, you will notice it no longer needs parameters — the data
      is already right there.

      That is the whole meaning of the word "object": data, plus the functions
      that operate on that data, living in the same place.
      Not inheritance. Not polymorphism. Just this.

    > But note
      Not every method belongs inside. Logic that coordinates several objects,
      or depends on external resources, rightly stays in a service class.
```

Every finding has five parts:

| | |
|---|---|
| **What happened** | facts and line numbers — verify it before believing it |
| **How you would have written it in C** | ← the key section. You don't lack the definition of encapsulation; you don't realise your Java is still C |
| **Why it matters** | concrete consequences, no lecturing |
| **Try this** | one executable step |
| **But note** | ← guards against overcorrection |

---

## "Unconfirmed" findings

Some judgements **fundamentally require understanding meaning**. Static analysis
cannot do them reliably, so the tool asks instead of asserting:

```
  [UNCONFIRMED] Item 2 · Loose parameters avoided

  (String title, String author)  —  2 classes, 2 methods

    > How to decide
      Can you give title, author a name?

          Yes  ->  then they should be a class
          No   ->  ignore this finding
```

This isn't a cop-out. **Coming up with the name is itself the act of deciding
whether these things are one concept** — which is object-oriented design.

---

## Design principles

**1 · A checklist, not a score.**
A score implies a universal yardstick. That yardstick is only needed at the
"how should this project be designed" layer — exactly the layer with no standard.
A 500-line inventory system and a 500-line game legitimately differ.

```
✗  You scored 34/100                       needs a universal standard
✓  (ip, port) crosses 3 classes,           a fact; needs none
   passed through 7 methods
```

**2 · Detection must be deterministic.**
One false positive and the reader never trusts you again. So:

- *what / where / which standard* → rule engine, pure static analysis, reproducible
- *why / how to fix* → explanation layer, optionally LLM-enhanced

**An LLM never participates in the judgement**, or it will hallucinate problems.
Run the same code twice and the findings are byte-identical.

**3 · Never auto-fix.**
The moment the tool edits your code for you, you stop thinking, and the entire
value is gone.

---

## Optional: LLM explanations

The tool ships with hand-written templates and **works completely without an LLM** —
offline, free, deterministic. If you want explanations tailored to your specific
code, plug in your own API key.

### What changes, and what doesn't

| | Without LLM | With LLM |
|---|---|---|
| Which findings, where, which standard | rule engine | **rule engine — identical** |
| The five prose sections | templates | rewritten for your code |
| Reproducibility | byte-identical | findings identical, wording varies |

**The LLM is architecturally forbidden from touching the judgement.** It receives an
already-decided finding and is instructed not to question it, not to add problems,
and not to mention anything not listed. Verify it yourself:

```bash
./checkup.sh samples --batch --summary                    # templates
./checkup.sh samples --batch --summary --config my.json   # LLM
# identical output
```

### Setup

```bash
cp oop-checkup.example.json oop-checkup.json   # gitignored
```

```jsonc
{
  "enabled": true,
  "baseUrl": "https://api.deepseek.com/v1",
  "apiKey":  "sk-...",           // or set OOPC_API_KEY
  "model":   "deepseek-chat",
  "timeoutMs": 15000
}
```

Lookup order: `--config <file>` → `./oop-checkup.json` → `~/.oop-checkup.json` →
environment variables `OOPC_API_KEY` / `OOPC_BASE_URL` / `OOPC_MODEL`.

Any **OpenAI-compatible** endpoint works:

| Provider | `baseUrl` | |
|---|---|---|
| DeepSeek | `https://api.deepseek.com/v1` | cheap |
| Ollama | `http://localhost:11434/v1` | **local, free, nothing leaves your machine** |
| OpenAI | `https://api.openai.com/v1` | |
| Qwen / Kimi / GLM | their compatible endpoints | free tiers available |

Force templates at any time with `--no-llm`.

### What gets sent

Only the finding metadata and **the code fragments that already appear in the
report** — class names, method signatures, field declarations, line numbers.
Whole source files are never transmitted.

Want zero data leaving your machine? Point `baseUrl` at Ollama.

### Failure policy

No config, timeout, HTTP error, malformed response, missing section — **every path
falls back to templates silently**. After three consecutive failures it stops trying,
so a dead endpoint cannot stall your report. Measured: an unreachable host costs
1.2 seconds in total.

**The report always comes out.**

---

## How this tool was validated

Not "it ran, ship it". Full records in [`runs/`](runs) and `PREREGISTRATION-v*.md`.

| Round | | What it caught |
|---|---|---|
| 001 | ❌ | Flagged Guava's 69 hand-written overloads as a smell |
| 002 | ❌ | **Blocked one act of post-hoc tuning**: a threshold that fell neatly between 64% and 57% would have made the numbers look great — rejected |
| 003 | ❌ | Utility classes can't be identified by name (`Strings` isn't `StringUtils`) |
| 004 | ⚠️ | 80.6% "passed", but the control group alone was 14% — failure masked by sample ratio |
| 005 | ❌ | **Blind test exposed sample bias**: not one of the first four student samples was a console-menu program, which is the most typical form of all |
| 006 | ✅ | **97.8%** (44/45); 100% on the blind set |

Three mechanisms, each catching something the others missed:

- **Control group** (Guava / JUnit5) — run only on student code, round 001's 13 findings look perfectly reasonable
- **Preregistration** (predictions frozen before every change) — blocked my own post-hoc tuning
- **Blind testing** (download, don't look, run) — exposed the sample bias

Remove any one and the defect ships.

---

## Known limits

- Items 2 and 7 rest on **semantic** judgement; they only report *unconfirmed*
- Item 1's external-access count is a **name-matching approximation** (no type resolution yet)
- Item 2 requires a clump to span **≥2 classes**. Code where one `XxxManager` does
  everything will under-report — a measured, unfixed gap
- One known residual false positive (Guava `Suppliers` internal fast path)
- 13 validation samples (2 mature libraries + 11 real student repositories) — still small
- **Java only**

---

## Roadmap

- [x] Six rules + checklist report
- [x] Six validation rounds (preregistration / control group / blind test)
- [x] Bilingual reports (`--lang zh|en`)
- [x] Optional LLM explanations (BYOK, OpenAI-compatible, zero added dependencies)
- [ ] Item 7 (Primitive Obsession)
- [ ] Web version: paste code, get the report
- [ ] IDE plugin

---

## Layout

```
src/main/java/com/ooc/
├── ir/          unified IR — swapping parsers never touches this
├── parse/       JavaParser → IR (the only parser-coupled code)
├── rules/       six rules, one file each
├── explain/     Explainer interface, TemplateExplainer, optional LlmExplainer
└── report/      checklist rendering

checkup.bat / .sh   launchers (encoding handled)
examples/           before / after demo code (compiles and runs)
testdata/           unit-test case for a rule
runs/               raw output and item-by-item verification, six rounds
PREREGISTRATION-v*.md   criteria and predictions frozen before each change
```

Adding a rule: implement `rules/Rule`, register it in `Main.RULES`. Nothing else changes.

> `runs/` and `PREREGISTRATION-v*.md` are archival records kept in Chinese.
> They document the validation process; the tool itself is fully bilingual.

---

## License

MIT
