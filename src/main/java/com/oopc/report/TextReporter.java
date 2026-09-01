package com.oopc.report;

import com.oopc.ir.Ir;
import com.oopc.rules.ScaleProfile;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checklist-style report.
 *
 * No overall score. A score implies a universal yardstick, and that yardstick
 * is only needed at the "how should this project be designed" layer — exactly
 * the layer that has no standard. The checklist only asserts what does have
 * standards, and every item cites its source.
 *
 * Layout: alignment and wrapping are computed by display width (CJK 2, ASCII 1);
 * identifiers are never split and punctuation never starts a line.
 */
public final class TextReporter {

    private static final String LINE = "════════════════════════════════════════════════════════════";
    private static final String THIN = "────────────────────────────────────────────────────────────";
    private static final int TITLE_COL_ZH = 38;
    private static final int TITLE_COL_EN = 46;
    private static final int TEXT_WIDTH_ZH = 66;
    private static final int TEXT_WIDTH_EN = 84;

    private static final String NO_LINE_START = "。，、；：？！）」』》〉】…·,.;:?!)]}>";

    private final int maxDetail;
    private final Lang lang;

    public TextReporter(int maxDetail, Lang lang) {
        this.maxDetail = maxDetail;
        this.lang = lang;
    }

    public void render(PrintStream out, String label, Ir.Project p,
                       ScaleProfile scale, Map<CheckItem, List<Finding>> results) {

        out.println(LINE);
        out.println(lang.pick("  面向对象转换检查表", "  OO Transition Checklist"));
        out.printf(lang.pick("  %s · %d 个文件 · %d 有效行 · %d 个类型%s%n",
                             "  %s · %d files · %d effective lines · %d types%s%n"),
                label, p.fileCount, p.effectiveLines, p.classes.size(),
                p.parseFailures > 0
                        ? lang.pick(" · 解析失败 ", " · parse failures: ") + p.parseFailures
                        : "");
        out.println(lang.pick("  规模档位：", "  Scale: ") + scale.describe(lang));
        out.println(LINE);
        out.println();

        int violated = 0, unconfirmed = 0;
        for (CheckItem item : CheckItem.values()) {
            String status;
            if (!item.implemented) {
                status = lang.pick("未实现", "not implemented");
            } else {
                List<Finding> fs = results.getOrDefault(item, List.of());
                long v = fs.stream().filter(f -> f.severity.isViolation()).count();
                long u = fs.size() - v;
                if (v > 0) {
                    status = lang.pick(v + " 处违反", v + " violation" + (v > 1 ? "s" : ""));
                    violated++;
                } else if (u > 0) {
                    status = lang.pick(u + " 处待确认", u + " unconfirmed");
                    unconfirmed++;
                } else {
                    status = lang.pick("通过", "pass");
                }
            }
            String head = "  " + item.no + ". " + item.title(lang) + " ";
            int col = lang.isEn() ? TITLE_COL_EN : TITLE_COL_ZH;
            out.println(head + dots(col - width(head)) + " " + status);
        }
        out.println();

        out.printf(lang.pick("  违反 %d 项，待确认 %d 项%n",
                             "  %d item(s) violated, %d item(s) unconfirmed%n"),
                violated, unconfirmed);
        if (!scale.conclusive) {
            out.println(lang.pick(
                    "  ⚠ 项目规模过小（< 80 有效行），以下发现仅供参考，不构成结论。",
                    "  ! Project too small (< 80 effective lines); findings below are"
                    + " indicative only."));
        }
        out.println();

        for (CheckItem item : CheckItem.values()) {
            if (!item.implemented) continue;
            List<Finding> fs = results.getOrDefault(item, List.of());
            int shown = 0;
            for (Finding f : fs) {
                if (shown >= maxDetail) {
                    out.printf(lang.pick("  … 检查项 %s 另有 %d 处，未展开%n%n",
                                         "  ... item %s has %d more finding(s), not expanded%n%n"),
                            item.no, fs.size() - shown);
                    break;
                }
                renderFinding(out, f);
                shown++;
            }
        }

        out.println(LINE);
        if (lang.isEn()) {
            out.println("  Notes");
            out.println("  - Items 2 and 7 rest on semantic judgement; they only report");
            out.println("    UNCONFIRMED and leave the decision to you.");
            out.println("  - Item 1's external-access count is a name-matching approximation");
            out.println("    (no type resolution yet).");
        } else {
            out.println("  说明");
            out.println("  · 检查项 2、7 的判据本质依赖语义理解，只输出「待确认」，由你自己判断");
            out.println("  · 检查项 1 的外部访问次数为启发式近似（未接入类型解析）");
        }
        out.println(LINE);
    }

    private void renderFinding(PrintStream out, Finding f) {
        out.println(THIN);
        out.printf(lang.pick("  [%s] 检查项 %s · %s%n", "  [%s] Item %s · %s%n"),
                f.severity.label(lang), f.item.no, f.item.title(lang));
        out.println("        "
                + (f.severity.isViolation()
                        ? lang.pick("违反标准：", "Standard violated: ")
                        : lang.pick("关联标准：", "Related standard: "))
                + f.item.standard(lang));
        out.println("        " + lang.pick("出处：", "Source: ") + f.item.source);
        out.println(THIN);
        out.println();
        emit(out, f.title, "  ");
        out.println();

        for (String loc : f.locations) {
            out.println("      " + loc);
        }
        out.println();

        Explanation e = f.explanation;
        if (e != null) {
            block(out, lang.pick("发生了什么", "What happened"), e.whatHappened);
            block(out, lang.pick("你在 C 里会怎么写", "How you would have written it in C"),
                    e.cInstinct);
            block(out, lang.pick("为什么是问题", "Why it matters"), e.whyItMatters);
            block(out, f.severity.isViolation()
                            ? lang.pick("试试", "Try this")
                            : lang.pick("怎么判断", "How to decide"),
                    e.suggestion);
            block(out, lang.pick("但要注意", "But note"), e.caveat);
        }
        out.println();
    }

    private void block(PrintStream out, String head, String text) {
        if (text == null || text.isEmpty()) return;
        out.println("    > " + head);
        for (String line : text.split("\n", -1)) {
            if (line.trim().isEmpty()) {
                out.println();
                continue;
            }
            int lead = 0;
            while (lead < line.length() && line.charAt(lead) == ' ') lead++;
            emit(out, line.substring(lead), "      " + " ".repeat(lead));
        }
        out.println();
    }

    /** Wrap by display width; never split identifiers, never start a line with punctuation. */
    private void emit(PrintStream out, String text, String indent) {
        int textWidth = lang.isEn() ? TEXT_WIDTH_EN : TEXT_WIDTH_ZH;
        int limit = Math.max(24, textWidth - width(indent));
        List<String> tokens = tokenize(text);
        StringBuilder cur = new StringBuilder();
        int w = 0;
        for (String t : tokens) {
            int tw = width(t);
            if (w > 0 && w + tw > limit) {
                out.println(indent + cur);
                cur.setLength(0);
                w = 0;
                if (t.equals(" ")) continue;
            }
            cur.append(t);
            w += tw;
        }
        if (cur.length() > 0) out.println(indent + cur);
    }

    private List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (isWordChar(c)) {
                word.append(c);
                continue;
            }
            if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
            if (NO_LINE_START.indexOf(c) >= 0 && !out.isEmpty()) {
                out.set(out.size() - 1, out.get(out.size() - 1) + c);
            } else {
                out.add(String.valueOf(c));
            }
        }
        if (word.length() > 0) out.add(word.toString());
        return out;
    }

    private boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || "_$.()[]{}<>/\\-+*&|=%@#'\"".indexOf(c) >= 0;
    }

    private static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) w += (c >= 0x2000) ? 2 : 1;
        return w;
    }

    private static String dots(int n) {
        return n <= 0 ? "" : ".".repeat(n);
    }

    /** One-line summary, used only for cross-sample comparison during validation. */
    public static String summaryLine(String label, Ir.Project p, ScaleProfile scale,
                                     Map<CheckItem, List<Finding>> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s", label));
        sb.append(String.format("L%6d C%4d ", p.effectiveLines, p.classes.size()));
        for (CheckItem item : CheckItem.values()) {
            if (!item.implemented) continue;
            List<Finding> fs = results.getOrDefault(item, List.of());
            long v = fs.stream().filter(f -> f.severity.isViolation()).count();
            long red = fs.stream().filter(f -> f.severity == Finding.Severity.RED).count();
            if (item.semantic) {
                sb.append(String.format("|%s:%d? ", item.no, fs.size()));
            } else {
                sb.append(String.format("|%s:%d(%d) ", item.no, v, red));
            }
        }
        return sb.toString();
    }
}
