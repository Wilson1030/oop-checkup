package com.ooc.report;

import com.ooc.ir.Ir;
import com.ooc.rules.ScaleProfile;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/** 文本报告渲染。阶段0不输出总分——只有两条规则，加权总分会制造虚假的科学感。 */
public final class TextReporter {

    private static final String LINE  = "════════════════════════════════════════════════════════════";
    private static final String THIN  = "────────────────────────────────────────────────────────────";

    private final int maxDetail;

    public TextReporter(int maxDetail) {
        this.maxDetail = maxDetail;
    }

    public void render(PrintStream out, String label, Ir.Project p,
                       ScaleProfile scale, Map<String, List<Finding>> byRule) {

        out.println(LINE);
        out.println("  面向对象思维体检 · 阶段0");
        out.println("  样本：" + label);
        out.println(LINE);
        out.printf("  文件 %d 个 · 有效代码 %d 行 · 类型 %d 个%s%n",
                p.fileCount, p.effectiveLines, p.classes.size(),
                p.parseFailures > 0 ? " · 解析失败 " + p.parseFailures + " 个" : "");
        out.println("  规模档位：" + scale.describe());
        out.println();

        // ---- 概览 ----
        out.println("  【检出概览】");
        for (Map.Entry<String, List<Finding>> e : byRule.entrySet()) {
            List<Finding> fs = e.getValue();
            long red = fs.stream().filter(f -> f.severity == Finding.Severity.RED).count();
            long yellow = fs.stream().filter(f -> f.severity == Finding.Severity.YELLOW).count();
            out.printf("    %-14s %2d 项   (严重 %d，中等 %d)%n",
                    e.getKey(), fs.size(), red, yellow);
        }
        out.println();

        if (!scale.conclusive) {
            out.println("  ⚠ 项目规模过小（< 80 有效行），以下发现仅供参考，不构成结论。");
            out.println();
        }

        // ---- 详情 ----
        for (Map.Entry<String, List<Finding>> e : byRule.entrySet()) {
            List<Finding> fs = e.getValue();
            if (fs.isEmpty()) {
                out.println(THIN);
                out.println("  [通过] " + e.getKey() + "  未发现问题");
                out.println();
                continue;
            }
            int shown = 0;
            for (Finding f : fs) {
                if (shown >= maxDetail) {
                    out.printf("  … 另有 %d 项同类发现，未展开%n%n", fs.size() - shown);
                    break;
                }
                renderFinding(out, f);
                shown++;
            }
        }

        out.println(LINE);
        out.println("  说明：R2 的「外部访问次数」为启发式近似 —— 阶段0 未接入类型解析，");
        out.println("        按成员名称匹配，同名字段分属不同类时会产生误差。");
        out.println(LINE);
    }

    private void renderFinding(PrintStream out, Finding f) {
        out.println(THIN);
        out.println("  " + f.severity.label + " " + f.ruleId + " · " + f.ruleName);
        out.println("  " + f.title);
        out.println();

        if (!f.locations.isEmpty()) {
            for (String loc : f.locations) {
                out.println("      " + loc);
            }
            out.println();
        }
        wrap(out, "发生了什么", f.whatHappened);
        wrap(out, "为什么是问题", f.whyItMatters);
        wrap(out, "试试", f.suggestion);
        wrap(out, "但要注意", f.caveat);
        out.println();
    }

    /** 中文按显示宽度折行 */
    private void wrap(PrintStream out, String head, String text) {
        if (text == null || text.isEmpty()) return;
        out.println("    ▸ " + head);
        int limit = 46;
        StringBuilder cur = new StringBuilder();
        int w = 0;
        for (char c : text.toCharArray()) {
            int cw = (c > 0x2000) ? 2 : 1;
            if (w + cw > limit) {
                out.println("      " + cur);
                cur.setLength(0);
                w = 0;
            }
            cur.append(c);
            w += cw;
        }
        if (cur.length() > 0) out.println("      " + cur);
        out.println();
    }

    /** 一行式摘要，用于多样本横向对比 */
    public static String summaryLine(String label, Ir.Project p, ScaleProfile scale,
                                     Map<String, List<Finding>> byRule) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-46s", trim(label, 46)));
        sb.append(String.format(" 行数%6d  类%4d ", p.effectiveLines, p.classes.size()));
        for (Map.Entry<String, List<Finding>> e : byRule.entrySet()) {
            List<Finding> fs = e.getValue();
            long red = fs.stream().filter(f -> f.severity == Finding.Severity.RED).count();
            sb.append(String.format(" | %s %2d (红%d)",
                    e.getKey().substring(0, Math.min(6, e.getKey().length())), fs.size(), red));
        }
        sb.append("  [").append(scale.scale).append("]");
        return sb.toString();
    }

    private static String trim(String s, int n) {
        return s.length() <= n ? s : "…" + s.substring(s.length() - n + 1);
    }
}
