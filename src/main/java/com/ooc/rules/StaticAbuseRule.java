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
 * 判据（PREREGISTRATION-v6.md）：
 *   A  存在 static 非 final 字段 —— 即全局变量
 *   排除  private static 且类型为自身类型的字段（单例惯例）
 *
 * N2（v6）：已移除判据 B（static 方法密度）。
 * 依据是纯粹的经验证据，无需论证：
 *   RUN-003  guava 12 条、junit5 16 条（工具类）      全部误报
 *   RUN-004  junit5 DynamicTest（静态工厂）            误报
 *   RUN-005  book-console BookQuery/BorrowRecordQuery（DAO） 误报
 * 三轮共 30 条检出，0 条正确。本检查项的全部真阳性均来自判据 A。
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
