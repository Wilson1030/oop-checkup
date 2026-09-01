package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 检查项 6 · 入口方法是否过度承担
 * 标准：Long Method 的特化（Fowler《重构》坏味道 #6）
 *
 * 判据（PREREGISTRATION-v3.md）：
 *   A  main 方法体 > 50 行 -> 中等；> 100 行 -> 严重
 *
 * 为什么单独把 main 拎出来，而不是做通用的「方法过长」：
 *   方法过长是通用代码质量问题，与「C 思维残留」只有间接关系，
 *   且在成熟库中普遍存在，必然造成误报。
 *   而「把全部逻辑写在 main 里」是「用 Java 写 C 程序」最直白的形态，
 *   既贴题，又因为库代码没有 main 而天然不会误报。
 */
public final class MainBloatRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.MAIN_BLOAT;
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            for (Ir.Method m : k.methods) {
                if (!m.name.equals("main") || !m.isStatic) continue;
                if (m.bodyLines <= 50) continue;

                Finding.Severity sev = m.bodyLines > 100
                        ? Finding.Severity.RED : Finding.Severity.YELLOW;

                Finding f = new Finding(item(), sev, lang.pick(
                        String.format("%s.main()  —  %d 行", k.name, m.bodyLines),
                        String.format("%s.main()  —  %d lines", k.name, m.bodyLines)));
                f.weight = m.bodyLines;
                f.facts.put("className", k.name);
                f.facts.put("lines", m.bodyLines);
                f.locations.add(shortFile(k.filePath) + ":" + m.line
                        + "   " + k.name + ".main()");
                findings.add(f);
            }
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
