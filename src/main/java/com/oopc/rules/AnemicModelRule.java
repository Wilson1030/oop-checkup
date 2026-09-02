package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item 1 - data and behaviour kept together
 * Standard: Anemic Domain Model (Martin Fowler, 2003)
 *
 * A class with fields and getters/setters but no behaviour, while the logic that
 * operates on those fields lives scattered in other classes -- this is treating
 * a class as a C struct and a service class as a set of C functions. The
 * separation of data and behaviour is the most fundamental leftover of
 * procedural thinking.
 *
 * Two exclusions added in v2 (correcting objective errors, not relaxing thresholds):
 *   E1 @interface annotation declaration -- an annotation is not a class; having
 *      no behaviour is its language definition. RUN-001's three JUnit5 false
 *      positives (TempDir / RepeatedTest / Timeout) were all of this kind.
 *   E2 non-public static nested class -- internal data nodes are deliberately anemic.
 *      RUN-001's Guava MoreObjects.ValueHolder is of this kind.
 *      (v2 said "private static", but that class is actually package-private; this
 *       was recorded in the RUN-002 analysis and corrected in v3)
 */
public final class AnemicModelRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.DATA_BEHAVIOR;
    }

    private static final Set<String> STANDARD_METHODS =
            new HashSet<>(Arrays.asList("toString", "equals", "hashCode", "compareTo", "clone"));

    private static final List<String> EXCLUDED_SUFFIX =
            Arrays.asList("DTO", "VO", "PO", "Entity", "Request", "Response", "Dto", "Vo", "Po");

    private static final Set<String> EXCLUDED_ANNOTATIONS =
            new HashSet<>(Arrays.asList("Entity", "Data", "Value", "Embeddable", "Table"));

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isEnum || k.isInterface || k.isAbstract || k.isRecord) continue;
            if (k.isAnnotation) continue;                              // E1
            if (k.isNested && k.isStatic && !k.isPublic) continue;      // E2
            if (nameExcluded(k.name)) continue;
            if (k.annotations.stream().anyMatch(EXCLUDED_ANNOTATIONS::contains)) continue;

            List<Ir.Field> inst = k.instanceFields();
            if (inst.size() < 3) continue;

            long business = k.methods.stream()
                    .filter(m -> !m.isConstructor)
                    .filter(m -> !isAccessor(m))
                    .filter(m -> !STANDARD_METHODS.contains(m.name))
                    .count();
            if (business > 0) continue;

            Set<String> members = memberNames(inst);
            Map<String, Integer> byClass = new LinkedHashMap<>();
            int total = 0;
            for (Ir.Access a : project.accesses) {
                if (a.fromClass.equals(k.qualifiedName)) continue;
                if (!members.contains(a.memberName)) continue;
                byClass.merge(a.fromClass, 1, Integer::sum);
                total++;
            }

            Finding.Severity sev = total > scale.anemicExternalSevere
                    ? Finding.Severity.RED : Finding.Severity.YELLOW;

            long accessors = k.methods.stream().filter(this::isAccessor).count();

            Finding f = new Finding(item(), sev, lang.pick(
                    String.format("%s  —  %d 个字段，%d 个 getter/setter，0 个业务方法",
                            k.name, inst.size(), accessors),
                    String.format("%s  —  %d fields, %d getters/setters, 0 business methods",
                            k.name, inst.size(), accessors)));
            f.weight = total;

            List<Map.Entry<String, Integer>> top = byClass.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(5)
                    .collect(Collectors.toList());

            f.facts.put("className", k.name);
            f.facts.put("fieldCount", inst.size());
            f.facts.put("accessorCount", (int) accessors);
            f.facts.put("externalAccess", total);
            f.facts.put("externalClasses", byClass.keySet().stream()
                    .map(this::simple).collect(Collectors.toList()));

            f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);
            for (Map.Entry<String, Integer> e : top) {
                f.locations.add("    " + simple(e.getKey())
                        + lang.pick("  访问它 " + e.getValue() + " 次",
                                    "  accesses it " + e.getValue() + " times"));
            }
            findings.add(f);
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    private Set<String> memberNames(List<Ir.Field> fields) {
        Set<String> s = new HashSet<>();
        for (Ir.Field f : fields) {
            s.add(f.name);
            String cap = f.name.isEmpty() ? "" :
                    Character.toUpperCase(f.name.charAt(0)) + f.name.substring(1);
            s.add("get" + cap);
            s.add("set" + cap);
            s.add("is" + cap);
        }
        return s;
    }

    private boolean isAccessor(Ir.Method m) {
        if (m.isConstructor) return false;
        boolean getter = (m.name.startsWith("get") || m.name.startsWith("is"))
                && m.params.isEmpty()
                && !m.returnType.equals("void")
                && m.bodyLines <= 3;
        boolean setter = m.name.startsWith("set")
                && m.params.size() == 1
                && m.returnType.equals("void")
                && m.bodyLines <= 3;
        return getter || setter;
    }

    private boolean nameExcluded(String name) {
        return EXCLUDED_SUFFIX.stream().anyMatch(name::endsWith);
    }

    private String simple(String qualified) {
        int i = qualified.lastIndexOf('.');
        return i < 0 ? qualified : qualified.substring(i + 1);
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
