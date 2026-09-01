# Validation records

Raw output and item-by-item verification for all six validation rounds.

| File | Content |
|---|---|
| `RUN-00N-ANALYSIS.md` | Results, verification and failure analysis for round N |
| `RUN-00N-<sample>.txt` | Raw tool output for that sample |
| `RUN-00N-summary.txt` | Cross-sample one-line comparison |

Summary of the six rounds:

| Round | Verdict | Key finding |
|---|---|---|
| 001 | fail | Flagged Guava's 69 hand-written overloads as a smell |
| 002 | fail | Data clumps are a semantic judgement; one act of post-hoc tuning was rejected |
| 003 | fail | Utility classes cannot be identified by name |
| 004 | marginal | 80.6% overall, but only 14% on the control group |
| 005 | fail | Blind testing exposed sample bias: no console-menu programs in the first four |
| 006 | **pass** | 97.8% (44/45); 100% on the blind set |

Each round's criteria and predictions were frozen in `../PREREGISTRATION-v*.md`
**before** any code was changed.

> These records are written in Chinese. They document the validation process,
> not tool usage — the tool itself is fully bilingual (`--lang zh|en`).
