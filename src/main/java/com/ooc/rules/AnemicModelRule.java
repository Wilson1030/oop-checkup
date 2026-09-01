package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.Finding;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * R2 · 贫血模型（Anemic Domain Model）
 *
 * 一个类只有字段和 getter/setter、没有任何行为，
 * 而操作这些字段的逻辑全都散落在别的类里 ——
 * 这就是把类当成 C 的 struct、把 Service 当成一组 C 函数。
 * 数据与行为分离，正是过程式思维最本质的残留。
 *
 * 阈值来自 PREREGISTRATION.md，已冻结。
 */
public final class AnemicModelRule implements Rule {

    @Override public String id()   { return "R2"; }
    @Override public String name() { return "贫血模型"; }

    private static final Set<String> STANDARD_METHODS =
            new HashSet<>(Arrays.asList("toString", "equals", "hashCode", "compareTo", "clone"));

    private static final List<String> EXCLUDED_SUFFIX =
            Arrays.asList("DTO", "VO", "PO", "Entity", "Request", "Response", "Dto", "Vo", "Po");

    private static final Set<String> EXCLUDED_ANNOTATIONS =
            new HashSet<>(Arrays.asList("Entity", "Data", "Value", "Embeddable", "Table"));

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isEnum || k.isInterface || k.isAbstract || k.isRecord) continue;
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

            // 统计外部访问
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

            Finding f = new Finding(id(), name(), sev,
                    k.name + "  —  " + inst.size() + " 个字段，"
                            + accessors + " 个 getter/setter，0 个业务方法");
            f.weight = total;
            f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);

            List<Map.Entry<String, Integer>> top = byClass.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(5)
                    .collect(Collectors.toList());
            for (Map.Entry<String, Integer> e : top) {
                f.locations.add("    " + simple(e.getKey()) + "  访问它 " + e.getValue() + " 次");
            }

            f.whatHappened =
                    k.name + " 有 " + inst.size() + " 个字段，却没有任何业务方法 —— "
                    + "它只是一个数据容器。"
                    + (total > 0
                        ? "而项目里有 " + byClass.size() + " 个其他类、共 " + total
                          + " 处在读写它的数据。"
                        : "");
            f.whyItMatters =
                    "数据在一个地方，操作数据的代码在另一个地方。"
                    + k.name + " 的字段含义只有外部那些类知道；"
                    + "哪天你改了字段，编译器不会告诉你外部哪些逻辑已经失效了。";
            f.suggestion =
                    "找一个只用到 " + k.name + " 自己字段的方法，把它搬进 " + k.name + "。"
                    + "搬完你会发现它不再需要参数了 —— 因为数据就在手边。这就是「对象」的意思。";
            f.caveat =
                    "不是所有逻辑都该搬进来。涉及多个对象协作、或依赖外部资源（数据库、网络、UI）的逻辑，"
                    + "留在 Service 里是对的。另外，如果 " + k.name
                    + " 本来就是纯粹的数据传输对象，那贫血是合理的。";

            findings.add(f);
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    /** 字段名 + 其对应的 getter/setter 名 */
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
