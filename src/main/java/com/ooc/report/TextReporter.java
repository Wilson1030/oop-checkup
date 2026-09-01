package com.ooc.report;

import com.ooc.ir.Ir;
import com.ooc.rules.ScaleProfile;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * 检查表式报告。
 *
 * 不输出总分 —— 总分隐含「存在一把普适的尺子」，而那把尺子只在
 * 「具体项目该怎么设计」这一层才需要，恰恰是没有标准的一层。
 * 检查表只断言有公认标准的那几层，每一项都可追溯到文献出处。
 */
public final class TextReporter {

    private static final String LINE = "════════════════════════════════════════════════════════════";
    private static final String THIN = "────────────────────────────────────────────────────────────";
    private static final int WIDTH = 58;

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

        // ---------------- 检查表 ----------------
        int violatedItems = 0;
        for (CheckItem item : CheckItem.values()) {
            String mark, status;
            if (!item.implemented) {
                mark = "·";
                status = "未实现";
            } else {
                List<Finding> fs = results.getOrDefault(item, List.of());
                if (fs.isEmpty()) {
                    mark = "✓";
                    status = "通过";
                } else {
                    mark = "✗";
                    status = fs.size() + " 处违反";
                    violatedItems++;
                }
            }
            out.printf("   %s  %s. %-26s %s%n", mark, item.no, item.title, status);
        }
        out.println();

        if (!scale.conclusive) {
            out.println("  ⚠ 项目规模过小（< 80 有效行），以下发现仅供参考，不构成结论。");
            out.println();
        }
        if (violatedItems == 0) {
            out.println("  已实现的检查项全部通过。");
            out.println();
        }

        // ---------------- 详情 ----------------
        for (CheckItem item : CheckItem.values()) {
            if (!item.implemented) continue;
            List<Finding> fs = results.getOrDefault(item, List.of());
            if (fs.isEmpty()) continue;

            int shown = 0;
            for (Finding f : fs) {
                if (shown >= maxDetail) {
                    out.printf("  … 本检查项另有 %d 处违反，未展开%n%n", fs.size() - shown);
                    break;
                }
                renderFinding(out, f);
                shown++;
            }
        }

        out.println(LINE);
        out.println("  说明：检查项 1 的「外部访问次数」为启发式近似 —— 当前未接入类型");
        out.println("        解析，按成员名称匹配，同名字段分属不同类时会产生误差。");
        out.println(LINE);
    }

    private void renderFinding(PrintStream out, Finding f) {
        out.println(THIN);
        out.printf("  ✗ 检查项 %s · %s          [%s]%n",
                f.item.no, f.item.title, f.severity.label);
        out.println("    违反标准：" + f.item.standard);
        out.println("    出处：" + f.item.source);
        out.println(THIN);
        out.println();
        out.println("  " + f.title);
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
            block(out, "试试", e.suggestion);
            block(out, "但要注意", e.caveat);
        }
        out.println();
    }

    /** 逐行输出并按显示宽度折行；保留文本自身的相对缩进（代码块） */
    private void block(PrintStream out, String head, String text) {
        if (text == null || text.isEmpty()) return;
        out.println("    ▸ " + head);
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) {
                out.println();
                continue;
            }
            int lead = 0;
            while (lead < line.length() && line.charAt(lead) == ' ') lead++;
            String indent = "      " + " ".repeat(lead);
            emitWrapped(out, line.substring(lead), indent);
        }
        out.println();
    }

    private void emitWrapped(PrintStream out, String text, String indent) {
        int limit = Math.max(20, WIDTH - indent.length() + 6);
        StringBuilder cur = new StringBuilder();
        int w = 0;
        for (char c : text.toCharArray()) {
            int cw = (c > 0x2000) ? 2 : 1;
            if (w + cw > limit) {
                out.println(indent + cur);
                cur.setLength(0);
                w = 0;
            }
            cur.append(c);
            w += cw;
        }
        if (cur.length() > 0) out.println(indent + cur);
    }

    /** 一行式摘要，仅用于验证阶段的多样本横向对比（非产品功能） */
    public static String summaryLine(String label, Ir.Project p, ScaleProfile scale,
                                     Map<CheckItem, List<Finding>> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-16s", label));
        sb.append(String.format(" 行%6d 类%4d ", p.effectiveLines, p.classes.size()));
        for (CheckItem item : CheckItem.values()) {
            if (!item.implemented) continue;
            List<Finding> fs = results.getOrDefault(item, List.of());
            long red = fs.stream().filter(x -> x.severity == Finding.Severity.RED).count();
            sb.append(String.format(" | 项%s %2d (严%d)", item.no, fs.size(), red));
        }
        sb.append("  [").append(scale.scale).append("]");
        return sb.toString();
    }
}
