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
 * 判据（PREREGISTRATION-v3.md）：
 *   A  存在 static 非 final 字段 —— 即全局变量
 *   B  非工具类中 static 方法占比 > 50% 且 static 方法数 >= 5
 *   排除  类名 *Utils / *Helper / *Constants 且无实例字段
 *   排除  private static 且类型为自身类型的字段（单例惯例）
 *
 * 正当性：static 非 final 字段在语义上等同于 C 的全局变量。
 * 这条判据极硬，几乎不存在解释空间。
 */
public final class StaticAbuseRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.STATIC_ABUSE;
    }

    private static final List<String> UTIL_SUFFIX =
            Arrays.asList("Utils", "Util", "Helper", "Helpers", "Constants", "Consts");

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
        List<Finding> findings = new ArrayList<>();

        for (Ir.Klass k : project.classes) {
            if (k.isInterface || k.isAnnotation) continue;
            if (isUtilityClass(k)) continue;

            // ---- 判据 A：全局可变状态 ----
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

            // ---- 判据 B：static 方法密度 ----
            List<Ir.Method> methods = k.methods.stream()
                    .filter(m -> !m.isConstructor)
                    .collect(Collectors.toList());
            long staticCount = methods.stream().filter(m -> m.isStatic).count();
            if (staticCount >= 5 && methods.size() > 0
                    && staticCount * 2 > methods.size()) {
                int pct = (int) (staticCount * 100 / methods.size());
                Finding f = new Finding(item(), Finding.Severity.YELLOW,
                        String.format("%s  —  %d 个方法中 %d 个是 static（%d%%）",
                                k.name, methods.size(), staticCount, pct));
                f.weight = pct;
                f.facts.put("kind", "static-density");
                f.facts.put("className", k.name);
                f.facts.put("staticCount", (int) staticCount);
                f.facts.put("totalCount", methods.size());
                f.facts.put("percent", pct);
                f.locations.add(shortFile(k.filePath) + ":" + k.line + "   class " + k.name);
                findings.add(f);
            }
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    /** 工具类：名称匹配惯例后缀，且没有实例字段 */
    private boolean isUtilityClass(Ir.Klass k) {
        boolean nameMatches = UTIL_SUFFIX.stream().anyMatch(k.name::endsWith);
        return nameMatches && k.instanceFields().isEmpty();
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
