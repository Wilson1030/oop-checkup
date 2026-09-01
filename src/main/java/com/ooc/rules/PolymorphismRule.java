package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查项 3 · 是否用多态替代类型判断
 * 标准：Switch Statements（Fowler《重构》坏味道 #11）
 *
 * 判据（PREREGISTRATION-v3.md）：
 *   A  同一 if-else 链中出现 >= 2 个 instanceof            -> 中等
 *   B  同一组 case 标签集合在 >= 2 处 switch 中重复出现     -> 严重
 *   C  同一组 instanceof 类型集合在 >= 2 个方法中重复出现   -> 严重
 *   排除  equals() 内的 instanceof（Java 规范要求的标准写法，在解析层已排除）
 *
 * 正当性：Fowler 对该坏味道的核心论述是「基于类型码的条件分派，新增类型时
 * 必须修改所有分派点」。单处分派可能是合理的（菜单、状态机），
 * 故 switch 只在重复出现时才判为违反。
 */
public final class PolymorphismRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.POLYMORPHISM;
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
        Map<String, List<Ir.TypeCheck>> bySignature = new LinkedHashMap<>();
        for (Ir.TypeCheck tc : project.typeChecks) {
            bySignature.computeIfAbsent(tc.kind + "|" + tc.signature, x -> new ArrayList<>()).add(tc);
        }

        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<String, List<Ir.TypeCheck>> e : bySignature.entrySet()) {
            List<Ir.TypeCheck> group = e.getValue();
            Ir.TypeCheck first = group.get(0);
            boolean repeated = group.size() >= 2;
            boolean isSwitch = first.kind == Ir.TypeCheck.Kind.SWITCH;

            // switch 单处不报：可能是菜单或状态机，属合理用法
            if (isSwitch && !repeated) continue;

            Finding.Severity sev = repeated ? Finding.Severity.RED : Finding.Severity.YELLOW;

            List<String> types = Arrays.asList(first.signature.split(","));
            String title = isSwitch
                    ? String.format("switch 分派 [%s]  在 %d 处重复出现",
                                    abbreviate(first.signature), group.size())
                    : String.format("instanceof 链 [%s]  %s",
                                    abbreviate(first.signature),
                                    repeated ? "在 " + group.size() + " 处重复出现"
                                             : first.branches + " 个类型分支");

            Finding f = new Finding(item(), sev, title);
            f.weight = group.size() * 10 + first.branches;
            f.facts.put("kind", isSwitch ? "switch" : "instanceof");
            f.facts.put("types", types);
            f.facts.put("repeatCount", group.size());
            f.facts.put("branches", first.branches);

            for (Ir.TypeCheck tc : group) {
                f.locations.add(String.format("%s:%d   %s.%s()",
                        shortFile(tc.file), tc.line, tc.klassSimple, tc.method));
            }
            findings.add(f);
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
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
