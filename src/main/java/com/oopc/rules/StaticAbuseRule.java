package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item 4 - static not abused
 * Standard: global mutable state breaks encapsulation (standard Java teaching material)
 *
 * Criteria (PREREGISTRATION-v6.md):
 *   A  a static, non-final field - i.e. a global variable
 *   Exclude  private static field whose type is the class itself (singleton idiom)
 *
 * N2 (v6): judgement B (static method density) has been removed.
 * The basis is purely empirical, requiring no argument:
 *   RUN-003  guava 12, junit5 16 (utility classes)          all false positives
 *   RUN-004  junit5 DynamicTest (static factory)            false positive
 *   RUN-005  book-console BookQuery/BorrowRecordQuery (DAO) false positive
 * 30 findings across three rounds, 0 correct. Every true positive for this item
 * came from judgement A.
 *
 * Justification: a static non-final field is semantically a C global variable.
 */
public final class StaticAbuseRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.STATIC_ABUSE;
    }

    private static final List<String> UTIL_SUFFIX =
            Arrays.asList("Utils", "Util", "Helper", "Helpers", "Constants", "Consts");

    /**
     * A utility class: neither instance fields nor instance methods.
     * Judged by structure, not by name (v4) -- java.lang.Math is not MathUtils.
     *
     * Note: in v4, judgement B already requires "has instance fields", so a
     * utility class never matches it. This method is kept only as documentation
     * and a self-check; it no longer participates in control flow.
     */
    boolean isUtilityClass(Ir.Klass k) {
        if (!k.instanceFields().isEmpty()) return false;
        return k.methods.stream()
                .filter(m -> !m.isConstructor)
                .allMatch(m -> m.isStatic);
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isInterface || k.isAnnotation) continue;

            // ---- Judgement A: global mutable state ----
            // Applies to all classes, including utility classes -- a static mutable
            // field inside a utility class is still a global variable.
            List<Ir.Field> globals = k.fields.stream()
                    .filter(f -> f.isStatic && !f.isFinal)
                    .filter(f -> !isSingletonHolder(k, f))
                    .collect(Collectors.toList());

            if (!globals.isEmpty()) {
                boolean anyExposed = globals.stream().anyMatch(f -> f.isPublic);
                Finding f = new Finding(item(),
                        anyExposed ? Finding.Severity.RED : Finding.Severity.YELLOW,
                        lang.pick(
                            String.format("%s  —  %d 个 static 非 final 字段（全局变量）",
                                    k.name, globals.size()),
                            String.format("%s  —  %d static non-final field(s) (global variables)",
                                    k.name, globals.size())));
                f.weight = globals.size() * 10 + (anyExposed ? 5 : 0);
                f.facts.put("kind", "global-field");
                f.facts.put("className", k.name);
                f.facts.put("fields", globals.stream()
                        .map(x -> (x.isPublic ? "public " : "") + "static " + x.type + " " + x.name)
                        .collect(Collectors.toList()));
                f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);
                for (Ir.Field g : globals) {
                    f.locations.add("    " + (g.isPublic ? "public " : "") + "static "
                            + g.type + " " + g.name);
                }
                findings.add(f);
            }
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    /** Singleton idiom: private static, type is the class itself. */
    private boolean isSingletonHolder(Ir.Klass k, Ir.Field f) {
        return !f.isPublic && f.type.equals(k.name);
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
