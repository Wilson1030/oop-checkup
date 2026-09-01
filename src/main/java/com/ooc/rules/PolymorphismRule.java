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
        // M1（v5）：多态只能用于「你能修改的类型」。
        // 对 String / Collection / boolean[] 做 instanceof 是无法避免的 ——
        // 你没法给它们添加方法，指出这类分派没有任何可行动性。
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
                // N1（v6）：switch 仅在标签为具名标识符时检测。
                // 一个「类型」必须有名字才能表达概念：case ById: 值得建一个类；
                // case "1": 里的 "1" 不代表任何领域概念 —— 它是序号（菜单项、索引）。
                // 对没有名字的数字做多态改造是不可能的，因为造不出对应的类。
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

            // switch 单处不报：可能是菜单或状态机，属合理用法
            if (isSwitch && !repeated) continue;

            List<String> types = filteredTypes.get(e.getKey());
            Finding.Severity sev = repeated ? Finding.Severity.RED : Finding.Severity.YELLOW;

            String title = isSwitch
                    ? String.format("switch 分派 [%s]  在 %d 处重复出现",
                                    abbreviate(String.join(",", types)), group.size())
                    : String.format("instanceof 链 [%s]  %s",
                                    abbreviate(String.join(",", types)),
                                    repeated ? "在 " + group.size() + " 处重复出现"
                                             : types.size() + " 个自定义类型分支");

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

    /** 去掉泛型参数，取基础类型名 */
    private String baseName(String type) {
        int i = type.indexOf('<');
        return i < 0 ? type : type.substring(0, i);
    }

    /** case 标签是否为纯数字字面量或纯数字字符串 */
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
