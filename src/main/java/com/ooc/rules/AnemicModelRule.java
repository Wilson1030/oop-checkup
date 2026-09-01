package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查项 1 · 数据与行为是否结合
 * 标准：Anemic Domain Model（Martin Fowler, 2003）
 *
 * 一个类只有字段和 getter/setter、没有任何行为，而操作这些字段的逻辑
 * 全部散落在别的类里 —— 这就是把类当成 C 的 struct、把 Service 当成
 * 一组 C 函数。数据与行为分离，是过程式思维最本质的残留。
 *
 * v2 新增两条排除（属修正客观错误，非放宽阈值）：
 *   E1 @interface 注解声明 —— 注解不是类，没有行为是其语言定义。
 *      RUN-001 中 JUnit5 的 TempDir / RepeatedTest / Timeout 三项误报皆属此。
 *   E2 非 public 的 static 嵌套类 —— 内部数据节点贫血是刻意设计。
 *      RUN-001 中 Guava 的 MoreObjects.ValueHolder 属此。
 *      （v2 误写为 private static，实际该类是包私有 static class；
 *        已在 RUN-002 分析中留痕，v3 修正）
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
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
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

            Finding f = new Finding(item(), sev, String.format(
                    "%s  —  %d 个字段，%d 个 getter/setter，0 个业务方法",
                    k.name, inst.size(), accessors));
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
                f.locations.add("    " + simple(e.getKey()) + "  访问它 " + e.getValue() + " 次");
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
