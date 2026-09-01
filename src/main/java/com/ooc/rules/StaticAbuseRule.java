package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查项 4 · static 是否被滥用
 * 标准：全局状态破坏封装（Java 教科书通识）
 *
 * 判据（PREREGISTRATION-v4.md）：
 *   A  存在 static 非 final 字段 —— 即全局变量
 *   B  类有 >= 1 个实例字段，且 static 方法占比 > 50%，且 static 方法 >= 5
 *   排除  工具类 = 无实例字段 且 无实例方法
 *   排除  private static 且类型为自身类型的字段（单例惯例）
 *
 * 为什么工具类按「结构」而不是「名字」判定（v4 修正）：
 *   一个类若既无实例字段、也无实例方法，它从设计上就不打算成为对象，
 *   而是一个函数集合（命名空间）。java.lang.Math 不叫 MathUtils，
 *   但没有人会说它「滥用 static」。名字不是工具类的本质特征。
 *   RUN-003 中 Guava 的 Ascii/Strings/Preconditions、JUnit5 的 Assertions
 *   共 28 项误报，全部源于按名字判定。
 *
 * 为什么判据 B 要求「有实例字段」（v4 修正）：
 *   它真正要捕捉的是自相矛盾的设计 —— 类声明了实例字段
 *   （说明它想成为一个对象），却几乎不用实例方法去操作它们。
 *   若类完全没有实例字段，那它要么是工具类（合理），
 *   要么是「用 static 字段模拟全局对象」—— 后者已由判据 A 捕捉。
 *
 * 正当性：static 非 final 字段在语义上等同于 C 的全局变量。
 */
public final class StaticAbuseRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.STATIC_ABUSE;
    }

    private static final List<String> UTIL_SUFFIX =
            Arrays.asList("Utils", "Util", "Helper", "Helpers", "Constants", "Consts");

    /**
     * 工具类：既无实例字段、也无实例方法。
     * 按结构判定，不看名字（v4）—— java.lang.Math 不叫 MathUtils。
     *
     * 注：v4 中判据 B 已要求「有实例字段」，工具类自然不会命中，
     * 故本方法仅作为概念留存与自检用途，不再参与控制流。
     */
    boolean isUtilityClass(Ir.Klass k) {
        if (!k.instanceFields().isEmpty()) return false;
        return k.methods.stream()
                .filter(m -> !m.isConstructor)
                .allMatch(m -> m.isStatic);
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isInterface || k.isAnnotation) continue;

            // ---- 判据 A：全局可变状态 ----
            // 适用于所有类，包括工具类 —— 工具类里的 static 可变字段同样是全局变量。
            List<Ir.Field> globals = k.fields.stream()
                    .filter(f -> f.isStatic && !f.isFinal)
                    .filter(f -> !isSingletonHolder(k, f))
                    .collect(Collectors.toList());

            if (!globals.isEmpty()) {
                boolean anyExposed = globals.stream().anyMatch(f -> f.isPublic);
                Finding f = new Finding(item(),
                        anyExposed ? Finding.Severity.RED : Finding.Severity.YELLOW,
                        String.format("%s  —  %d 个 static 非 final 字段（全局变量）",
                                k.name, globals.size()));
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

            // ---- 判据 B：实例字段与 static 方法共存的矛盾设计 ----
            if (k.instanceFields().isEmpty()) continue;   // v4：无实例字段则不适用
            List<Ir.Method> methods = k.methods.stream()
                    .filter(m -> !m.isConstructor)
                    .collect(Collectors.toList());
            long staticCount = methods.stream().filter(m -> m.isStatic).count();
            if (staticCount >= 5 && methods.size() > 0
                    && staticCount * 2 > methods.size()) {
                int pct = (int) (staticCount * 100 / methods.size());
                Finding f = new Finding(item(), Finding.Severity.YELLOW,
                        String.format("%s  —  %d 个实例字段，但 %d 个方法中 %d 个是 static（%d%%）",
                                k.name, k.instanceFields().size(), methods.size(), staticCount, pct));
                f.weight = pct;
                f.facts.put("kind", "static-density");
                f.facts.put("className", k.name);
                f.facts.put("staticCount", (int) staticCount);
                f.facts.put("totalCount", methods.size());
                f.facts.put("percent", pct);
                f.facts.put("instanceFieldCount", k.instanceFields().size());
                f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);
                findings.add(f);
            }
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    /** 单例惯例：private static 且类型就是自身 */
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
