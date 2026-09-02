package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item 3 - polymorphism instead of type checks
 * Standard: Switch Statements (Fowler, Refactoring - smell #11)
 *
 * Criteria (PREREGISTRATION-v3.md):
 *   A  >= 2 instanceof in one if-else chain              -> MINOR
 *   B  the same set of case labels repeated in >= 2 switches  -> MAJOR
 *   C  the same set of instanceof types repeated in >= 2 methods -> MAJOR
 *   Exclude  instanceof inside equals() (canonical idiom; already excluded at parse)
 *
 * Justification: Fowler's core point is "type-code based dispatch means you
 * must modify every dispatch point when adding a type". A single dispatch may
 * be legitimate (a menu, a state machine), so a switch is only flagged when
 * it is repeated.
 */
public final class PolymorphismRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.POLYMORPHISM;
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        // M1 (v5): polymorphism only applies to types you can modify.
        // instanceof on String / Collection / boolean[] is unavoidable -- you
        // cannot add methods to them, so flagging it offers no actionable step.
        Set<String> projectTypes = project.classes.stream()
                .map(k -> k.name).collect(Collectors.toSet());

        Map<String, List<Ir.TypeCheck>> bySignature = new LinkedHashMap<>();
        Map<String, List<String>> filteredTypes = new LinkedHashMap<>();

        for (Ir.TypeCheck tc : project.typeChecks) {
            String sig;
            List<String> kept;
            if (tc.kind == Ir.TypeCheck.Kind.INSTANCEOF_CHAIN) {
                kept = tc.rawTypes.stream()
                        .filter(t -> !t.contains("["))                 // 排除数组类型
                        .filter(t -> projectTypes.contains(baseName(t))) // 仅保留项目自定义类型
                        .collect(Collectors.toList());
                if (kept.size() < 2) continue;
                sig = String.join(",", kept);
            } else {
                // N1 (v6): detect a switch only when its labels are named identifiers.
                // A "type" must have a name to express a concept: case ById: deserves
                // a class; the "1" in case "1": carries no domain concept -- it is an
                // ordinal (menu item, index). Polymorphic refactoring of a nameless
                // number is impossible, because there is no class to create.
                if (tc.rawTypes.stream().allMatch(this::isNumericLabel)) continue;
                kept = Collections.emptyList();
                sig = tc.signature;
            }
            String key = tc.kind + "|" + sig;
            bySignature.computeIfAbsent(key, x -> new ArrayList<>()).add(tc);
            filteredTypes.putIfAbsent(key, kept.isEmpty()
                    ? Arrays.asList(sig.split(",")) : kept);
        }

        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<String, List<Ir.TypeCheck>> e : bySignature.entrySet()) {
            List<Ir.TypeCheck> group = e.getValue();
            Ir.TypeCheck first = group.get(0);
            boolean repeated = group.size() >= 2;
            boolean isSwitch = first.kind == Ir.TypeCheck.Kind.SWITCH;

            // A single switch is not reported: it may be a menu or state machine, which is legitimate.
            if (isSwitch && !repeated) continue;

            List<String> types = filteredTypes.get(e.getKey());
            Finding.Severity sev = repeated ? Finding.Severity.RED : Finding.Severity.YELLOW;

            String abbr = abbreviate(String.join(",", types));
            String title;
            if (isSwitch) {
                title = lang.pick(
                        String.format("switch 分派 [%s]  在 %d 处重复出现", abbr, group.size()),
                        String.format("switch dispatch on [%s]  —  repeated in %d places",
                                abbr, group.size()));
            } else if (repeated) {
                title = lang.pick(
                        String.format("instanceof 链 [%s]  在 %d 处重复出现", abbr, group.size()),
                        String.format("instanceof chain on [%s]  —  repeated in %d places",
                                abbr, group.size()));
            } else {
                title = lang.pick(
                        String.format("instanceof 链 [%s]  %d 个自定义类型分支", abbr, types.size()),
                        String.format("instanceof chain on [%s]  —  %d project-defined type branches",
                                abbr, types.size()));
            }

            Finding f = new Finding(item(), sev, title);
            f.weight = group.size() * 10 + types.size();
            f.facts.put("kind", isSwitch ? "switch" : "instanceof");
            f.facts.put("types", types);
            f.facts.put("repeatCount", group.size());
            f.facts.put("branches", types.size());

            for (Ir.TypeCheck tc : group) {
                f.locations.add(String.format("%s:%d   %s.%s()",
                        shortFile(tc.file), tc.line, tc.klassSimple, tc.method));
            }
            findings.add(f);
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    /** Strips generic bounds, returns the base type name. */
    private String baseName(String type) {
        int i = type.indexOf('<');
        return i < 0 ? type : type.substring(0, i);
    }

    /** Whether the case label is a pure numeric literal or a numeric string. */
    private boolean isNumericLabel(String label) {
        String s = label.trim();
        if (s.length() >= 2
                && ((s.startsWith("\"") && s.endsWith("\""))
                 || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
        }
        return s.matches("-?\\d+");
    }

    private String abbreviate(String sig) {
        List<String> parts = Arrays.stream(sig.split(",")).collect(Collectors.toList());
        if (parts.size() <= 4) return String.join(", ", parts);
        return String.join(", ", parts.subList(0, 4)) + ", …共 " + parts.size() + " 项";
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
