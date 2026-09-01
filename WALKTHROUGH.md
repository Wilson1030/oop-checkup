# Walkthrough

Get the whole thing running from scratch. Every step lists its **expected output** —
if yours doesn't match, something went wrong.

> Copy-paste friendly. Problems are covered at the bottom.

---

## Step 0 · Check your environment

```bash
java -version
mvn -v
git --version
```

**Expected**: JDK 17 or newer, Maven 3.x, git 2.x.

- No `java` → install JDK 17+
- `mvn` not found → see [troubleshooting ①](#-mvn-command-not-found)
- Only want to *use* it, not modify it → `java` alone is enough; skip to step 2

---

## Step 1 · Build

```bash
cd oo-checkup
mvn package
```

First run downloads JavaParser, roughly 1–2 minutes.

**Expected**: `BUILD SUCCESS`, and `target/oo-checkup.jar` (~1.5 MB).

`BUILD FAILURE` → see [troubleshooting ②](#-build-failure)

---

## Step 2 · Run the demo: the bad version

Use the launcher — console encoding is already handled:

```powershell
.\checkup.bat examples\before          # Windows
```
```bash
./checkup.sh examples/before           # macOS / Linux
```

> In PowerShell the `.\` prefix is required. In CMD you can just type `checkup ...`.

**Expected** (checklist section):

```
  1. 数据与行为是否结合 .............. 2 处违反
  2. 是否避免散装参数传递 ............ 1 处待确认
  3. 是否用多态替代类型判断 .......... 1 处违反
  4. static 是否被滥用 ............... 2 处违反
  5. 封装是否完整 .................... 1 处违反
  6. 入口方法是否过度承担 ............ 1 处违反
  7. 是否避免基本类型偏执 ............ 未实现

  违反 5 项，待确认 1 项
```

Want it in English? Add `--lang en`:

```powershell
.\checkup.bat examples\before --lang en
```

```
  1. Data and behaviour kept together ....... 2 violations
  2. Loose parameters avoided ............... 1 unconfirmed
  3. Polymorphism instead of type checks .... 1 violation
  4. static not abused ...................... 2 violations
  5. Encapsulation intact ................... 1 violation
  6. Entry point not overloaded ............. 1 violation
  7. Primitive obsession avoided ............ not implemented

  5 item(s) violated, 1 item(s) unconfirmed
```

Garbled characters → see [troubleshooting ③](#-garbled-characters)

### See everything

Each item expands at most 3 findings by default:

```powershell
.\checkup.bat examples\before --detail 20
```

### Recommended: write it to a file

```powershell
.\checkup.bat examples\before --detail 20 > before.txt
```

File output is never affected by console encoding. Open it in an editor.

---

## Step 3 · Now the refactored version

```powershell
.\checkup.bat examples\after
```

**Expected**: every implemented item passes, `违反 0 项`.

Same system, same behaviour, only restructured.

**This step matters more than step 2** — it proves the tool stays quiet on good code.

### Diff the two directories to see what changed

| Item | `before` | `after` |
|---|---|---|
| 1 data & behaviour | `Book.java` has only getters/setters | `Book.java` has `lend()` / `giveBack()` / `isAvailable()` |
| 3 polymorphism | two `switch` blocks in `SearchService` | `SearchStrategy` interface + 3 implementations |
| 4 static | `LibraryService` all-static with globals | `Library` is an ordinary object |
| 5 encapsulation | `Book.title` is `public` | all `private final` |
| 6 main | `Main.main()` 85 lines | `Main.main()` 12 lines |

Both compile — verify it yourself:

```bash
javac -encoding UTF-8 -d out/before examples/before/library/*.java
javac -encoding UTF-8 -d out/after  examples/after/library/*.java
```

---

## Step 4 · Verify determinism yourself

Don't take my word for it:

```powershell
.\checkup.bat examples\before --detail 20 > r1.txt
.\checkup.bat examples\before --detail 20 > r2.txt
fc r1.txt r2.txt
```

**Expected**: `FC: no differences encountered`

On macOS / Linux / Git Bash use `diff r1.txt r2.txt` — no output means identical.

### Why this holds

| | |
|---|---|
| Randomness | never used |
| LLM | not wired in (once it is, **only the prose changes**; findings and line numbers do not) |
| Timestamps | never printed |
| Hash iteration order | `LinkedHashMap` / `TreeSet` throughout |

The two languages also produce **identical finding counts** — only the wording differs.

---

## Step 5 · Run it on your own code

```powershell
.\checkup.bat "C:\path\to\your\project" --detail 20 > report.txt
```

**Quote the path** if it contains spaces or non-ASCII characters.

Point it at the **project root**; it recurses and finds every `.java`.

### How to read the report

Look at the checklist first, then **fix only the first MAJOR finding**.

Each finding has five parts:

```
> What happened                        facts and line numbers — verify before believing
> How you would have written it in C   <- read this one first
> Why it matters                       concrete consequences
> Try this                             one executable step
> But note                             <- do not skip this
```

**Always read "But note".** For example item 1 suggests moving methods into the
entity class — but logic that touches a database, the network or the UI rightly
stays in a service class. That is what the section is for.

---

## Step 6 · Reproduce the validation (optional)

To check that "97.8% accuracy" isn't just a claim:

```bash
mkdir -p samples && cd samples
git clone --depth 1 --filter=blob:none --sparse https://github.com/google/guava.git guava
cd guava && git sparse-checkout set guava/src/com/google/common/base && cd ../..

./checkup.sh samples/guava --summary
```

**Expected**:

```
guava   L  6360 C 133 |1:0(0) |2:61? |3:1(0) |4:1(0) |5:0(0) |6:0(0)
```

Reading: `|item:violations(major)`, and `?` marks unconfirmed items.

**What to look at**: items 1, 5 and 6 are all zero. Guava is widely regarded as
well-written code — **the tool should stay essentially silent on it**. That is this
project's survival line.

The 61 on item 2 are *unconfirmed*, not violations (it is a semantic judgement,
so the tool asks rather than asserts).

### Full records

```
runs/RUN-001-ANALYSIS.md ... RUN-006-ANALYSIS.md   results and item-by-item verification
PREREGISTRATION.md ... -v6.md                      criteria and predictions, frozen before each change
```

Read them in order and you get the whole story, including five failed rounds and one
occasion where a threshold would have made the numbers look great and was rejected.

> These archives are written in Chinese. They document process, not usage —
> the tool itself is fully bilingual.

---

## Troubleshooting

### ① `mvn` command not found

Maven's environment variables only apply to **newly opened terminals**. Close and reopen.

Still failing? Use the full path:

```powershell
C:\Users\<you>\tools\apache-maven-3.9.16\bin\mvn package
```

### ② `BUILD FAILURE`

Look for `Could not resolve dependencies` — that means the JavaParser download failed.

If your network is slow, create `~/.m2/settings.xml`
(Windows: `C:\Users\<you>\.m2\settings.xml`):

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### ③ Garbled characters

**Won't happen if you use `checkup.bat` / `checkup.sh`** — they handle it.

Calling `java` directly:

```powershell
chcp 65001                                  # once per terminal window
java -jar target\oo-checkup.jar <path>
```

Or just write to a file — **simplest, immune to console settings**:

```powershell
java -jar target\oo-checkup.jar <path> > report.txt
```

### ④ PowerShell: `Could not find or load main class .encoding=UTF-8`

Full error:

```
Error: Could not find or load main class .encoding=UTF-8
Caused by: java.lang.ClassNotFoundException: /encoding=UTF-8
```

**Cause**: PowerShell splits `-Dfile.encoding=UTF-8` at the dot:

```
unquoted  ->  [-Dfile]  [.encoding=UTF-8]  [-jar]  ...
                         ^ java treats this as the main class name
quoted    ->  [-Dfile.encoding=UTF-8]  [-jar]  ...   correct
```

CMD does not do this; only PowerShell.

**Three fixes**:

```powershell
.\checkup.bat <path>                                              # 1. use the launcher
java "-Dfile.encoding=UTF-8" -jar target\oo-checkup.jar <path>    # 2. quote it
java -jar target\oo-checkup.jar <path> > report.txt               # 3. write to a file
```

### ⑤ `Illegal char <"> at index 0`

A quote ended up inside the path. Check for nested quotes, or for `\"` escaping
if you are calling from Git Bash.

### ⑥ "Project too small"

Below 80 effective lines the tool draws no conclusions — a 50-line program with
everything in `main` is perfectly reasonable, and reporting it would only mislead.

### ⑦ No findings at all

- Confirm the path actually contains `.java` files
- The report header prints `N files · N effective lines`; if it says 0, the path is wrong
- Test directories are skipped by default — add `--include-tests`

### ⑧ You think a finding is wrong

**You may well be right.** Known limits:

- Item 1's external-access count is a name-matching approximation; identically named
  fields in different classes get conflated
- Items 2 and 7 are semantic judgements, which is why they only report *unconfirmed*
- Item 2 requires a clump to span **≥2 classes**. If your code puts everything in one
  `XxxManager`, a genuine clump passed through eight methods will not be reported —
  a measured, deliberate gap
- One known residual false positive (Guava `Suppliers` internal fast path)

When filing an issue please include **the triggering code and why you think it
shouldn't fire**. A criterion has to be justifiable independently of any particular
sample — that is the rule this project runs on.

---

## Cheat sheet

```powershell
mvn package                                     # build

.\checkup.bat <path>                            # run
.\checkup.bat <path> --lang en                  # English report
.\checkup.bat <path> --detail 20                # expand everything
.\checkup.bat <path> --detail 20 > report.txt   # write to file (recommended)
.\checkup.bat <path> --summary                  # one line
.\checkup.bat <parent> --batch --summary        # compare many projects
```

macOS / Linux: replace `.\checkup.bat` with `./checkup.sh` and `\` with `/`.

| Status | Meaning |
|---|---|
| `pass` | nothing found |
| `N violations` | violates the cited standard, traceable to a source |
| `N unconfirmed` | semantic judgement — the tool asks, you decide |
| `not implemented` | rule not built yet |
