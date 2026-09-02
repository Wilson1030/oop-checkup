package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item 5 - encapsulation intact
 * Standard: public mutable fields expose internals
 *
 * Criteria (PREREGISTRATION-v3.md):
 *   A  public, non-final, non-static instance field
 *   Exclude  DTO / VO / PO / Entity naming and annotations (reuse item 1's exclusions)
 *   MAJOR  >= 3 in the same class
 *
 * Justification: a public mutable field directly exposes internal representation; the
 * textbook definition of broken encapsulation. The hardest of all seven items -- it
 * should not exist in a mature library.
 */
public final class EncapsulationRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.ENCAPSULATION;
    }

    private static final List<String> EXCLUDED_SUFFIX =
            Arrays.asList("DTO", "VO", "PO", "Entity", "Request", "Response", "Dto", "Vo", "Po");

    private static final Set<String> EXCLUDED_ANNOTATIONS =
            new HashSet<>(Arrays.asList("Entity", "Data", "Value", "Embeddable", "Table"));

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isInterface || k.isAnnotation || k.isRecord || k.isEnum) continue;
            if (EXCLUDED_SUFFIX.stream().anyMatch(k.name::endsWith)) continue;
            if (k.annotations.stream().anyMatch(EXCLUDED_ANNOTATIONS::contains)) continue;

            List<Ir.Field> exposed = k.fields.stream()
                    .filter(f -> f.isPublic && !f.isFinal && !f.isStatic)
                    .collect(Collectors.toList());
            if (exposed.isEmpty()) continue;

            Finding.Severity sev = exposed.size() >= 3
                    ? Finding.Severity.RED : Finding.Severity.YELLOW;

            Finding f = new Finding(item(), sev, lang.pick(
                    String.format("%s  —  %d 个 public 可变字段", k.name, exposed.size()),
                    String.format("%s  —  %d public mutable field(s)", k.name, exposed.size())));
            f.weight = exposed.size();
            f.facts.put("className", k.name);
            f.facts.put("fields", exposed.stream()
                    .map(x -> "public " + x.type + " " + x.name)
                    .collect(Collectors.toList()));

            f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);
            for (Ir.Field x : exposed) {
                f.locations.add("    public " + x.type + " " + x.name);
            }
            findings.add(f);
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
