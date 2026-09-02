package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Item 6 - entry point not overloaded
 * Standard: Long Method (main-specific) (Fowler, Refactoring - smell #6)
 *
 * Criteria (PREREGISTRATION-v3.md):
 *   A  main body > 50 lines -> MINOR; > 100 lines -> MAJOR
 *
 * Why main is singled out instead of a generic "method too long":
 *   A long method is a generic code-quality issue, only indirectly related to
 *   "residual C thinking", and it is common in mature libraries, so it would
 *   inevitably cause false positives.
 *   "All logic in main" is the most direct shape of writing C in Java: it is on
 *   point, and because library code has no main, it never falsely fires.
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
