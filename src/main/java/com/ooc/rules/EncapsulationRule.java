package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;
import com.ooc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查项 5 · 封装是否完整
 * 标准：public 可变字段暴露内部表示
 *
 * 判据（PREREGISTRATION-v3.md）：
 *   A  存在 public 非 final 非 static 实例字段
 *   排除  DTO / VO / PO / Entity 等命名与注解（沿用检查项 1 的排除集）
 *   严重  同一类中 >= 3 个
 *
 * 正当性：public 可变字段直接暴露内部表示，是封装破损的教科书定义。
 * 这是全部七项中最硬的一条 —— 成熟库中不应存在。
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
