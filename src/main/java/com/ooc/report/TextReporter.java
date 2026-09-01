package com.ooc.report;

import com.ooc.ir.Ir;
import com.ooc.rules.ScaleProfile;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 检查表式报告。
 *
 * 不输出总分 —— 总分隐含「存在一把普适的尺子」，而那把尺子只在
 * 「具体项目该怎么设计」这一层才需要，恰恰是没有标准的一层。
 * 检查表只断言有公认标准的那几层，每一项都可追溯到文献出处。
 *
 * 排版：按显示宽度（中文 2、ASCII 1）计算对齐与折行；
 * 折行时不拆开标识符，也不把标点留在行首。
 */
public final class TextReporter {

    private static final String LINE = "════════════════════════════════════════════════════════════";
    private static final String THIN = "────────────────────────────────────────────────────────────";
    private static final int TITLE_COL = 34;
    private static final int TEXT_WIDTH = 62;

    /** 不得出现在行首的标点 */
    private static final String NO_LINE_START = "。，、；：？！）」』》〉】…·,.;:?!)]}>";

    private final int maxDetail;

    public TextReporter(int maxDetail) {
        this.maxDetail = maxDetail;
    }

    public void render(PrintStream out, String label, Ir.Project p,
                       ScaleProfile scale, Map<CheckItem, List<Finding>> results) {

        out.println(LINE);
        out.println("  面向对象转换检查表");
        out.printf("  %s · %d 个文件 · %d 有效行 · %d 个类型%s%n",
                label, p.fileCount, p.effectiveLines, p.classes.size(),
                p.parseFailures > 0 ? " · 解析失败 " + p.parseFailures : "");
        out.println("  规模档位：" + scale.describe());
        out.println(LINE);
        out.println();

        int violated = 0, unconfirmed = 0;
        for (CheckItem item : CheckItem.values()) {
            String status;
            if (!item.implemented) {
                status = "未实现";
            } else {
                List<Finding> fs = results.getOrDefault(item, List.of());
                long v = fs.stream().filter(f -> f.severity.isViolation()).count();
                long u = fs.size() - v;
                if (v > 0) {
                    status = v + " 处违反";
                    violated++;
                } else if (u > 0) {
                    status = u + " 处待确认";
                    unconfirmed++;
                } else {
                    status = "通过";
                }
            }
            String head = "  " + item.no + ". " + item.title + " ";
            out.println(head + dots(TITLE_COL - width(head)) + " " + status);
        }
        out.println();

        out.printf("  违反 %d 项，待确认 %d 项%n", violated, unconfirmed);
        if (!scale.conclusive) {
            out.println("  ⚠ 项目规模过小（< 80 有效行），以下发现仅供参考，不构成结论。");
        }
        out.println();

        for (CheckItem item : CheckItem.values()) {
            if (!item.implemented) continue;
            List<Finding> fs = results.getOrDefault(item, List.of());
            int shown = 0;
            for (Finding f : fs) {
                if (shown >= maxDetail) {
                    out.printf("  … 检查项 %s 另有 %d 处，未展开%n%n", item.no, fs.size() - shown);
                    break;
                }
                renderFinding(out, f);
                shown++;
            }
        }

        out.println(LINE);
        out.println("  说明");
        out.println("  · 检查项 2、7 的判据本质依赖语义理解，只输出「待确认」，由你自己判断");
        out.println("  · 检查项 1 的外部访问次数为启发式近似（未接入类型解析）");
        out.println(LINE);
    }

    private void renderFinding(PrintStream out, Finding f) {
        out.println(THIN);
        out.printf("  [%s] 检查项 %s · %s%n", f.severity.label, f.item.no, f.item.title);
        out.println("        " + (f.severity.isViolation() ? "违反标准：" : "关联标准：") + f.item.standard);
        out.println("        出处：" + f.item.source);
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
            block(out, "发生了什么", e.whatHappened);
            block(out, "你在 C 里会怎么写", e.cInstinct);
            block(out, "为什么是问题", e.whyItMatters);
            block(out, f.severity.isViolation() ? "试试" : "怎么判断", e.suggestion);
            block(out, "但要注意", e.caveat);
        }
        out.println();
    }

    private void block(PrintStream out, String head, String text) {
        if (text == null || text.isEmpty()) return;
        out.println("    ▸ " + head);
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

    /** 按显示宽度折行；不拆开标识符，不把标点留在行首 */
    private void emit(PrintStream out, String text, String indent) {
        int limit = Math.max(24, TEXT_WIDTH - width(indent));
        List<String> tokens = tokenize(text);
        StringBuilder cur = new StringBuilder();
        int w = 0;
        for (String t : tokens) {
            int tw = width(t);
            if (w > 0 && w + tw > limit) {
                out.println(indent + cur);
                cur.setLength(0);
                w = 0;
                if (t.equals(" ")) continue;      // 行首不留空格
            }
            cur.append(t);
            w += tw;
        }
        if (cur.length() > 0) out.println(indent + cur);
    }

    /**
     * 切分为不可拆的最小单元：
     *   连续的 ASCII 词字符（标识符、数字、括号）视为一个整体
     *   中文逐字可断
     *   行首禁则标点并入前一个 token
     */
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

    /** 一行式摘要，仅用于验证阶段的多样本横向对比（非产品功能） */
    public static String summaryLine(String label, Ir.Project p, ScaleProfile scale,
                                     Map<CheckItem, List<Finding>> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-15s", label));
        sb.append(String.format("行%6d 类%4d ", p.effectiveLines, p.classes.size()));
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
