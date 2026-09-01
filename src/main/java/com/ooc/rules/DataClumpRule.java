package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;
import com.ooc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 检查项 2 · 是否避免散装参数传递
 * 标准：Data Clump / Long Parameter List（Fowler《重构》坏味道 #3 #4）
 *
 * 判据（v2，全部条件须同时满足，见 PREREGISTRATION-v2.md）：
 *   C1 长度 >= 2 的连续参数子序列，类型与名称均一致
 *   C2 出现在 >= 2 个不同的类中      <- v2 新增
 *   C3 涉及 >= 2 个不同的方法名      <- v2 新增
 *   C4 出现次数 >= 2
 *   C5 非全部单字母参数名
 *
 * C2 / C3 的正当性独立于样本：参数团的危害在于「同一组数据被拆散后，
 * 跨越多个不相关的地方传递」。同一个类内部的辅助方法链共享上下文，
 * 以及同名方法的重载家族，都不具备这个特征 —— 它们是正常甚至优秀的设计。
 *
 * RUN-002 证明：本规则的判据本质上是**语义性**的 ——
 * 真正的判据是「这组参数能否命名一个有意义的概念」，
 * 而这无法从语法结构推导（同样的语法形态，语义上可能是也可能不是）。
 *
 * 因此本规则不断言「违反」，只输出「待确认」，把判断权交还学生。
 * 这不是逃避 —— 教学上反而更好：起名这个动作本身就是在逼学生判断
 * 「这几个东西是不是同一个概念」，那正是我们希望他学会的思考。
 */
public final class DataClumpRule implements Rule {

    @Override
    public CheckItem item() {
        return CheckItem.PARAM_CLUMP;
    }

    private static final class Occ {
        final String klass, klassSimple, methodName, file, sig;
        final int line;

        Occ(String klass, String klassSimple, String methodName, String file, String sig, int line) {
            this.klass = klass;
            this.klassSimple = klassSimple;
            this.methodName = methodName;
            this.file = file;
            this.sig = sig;
            this.line = line;
        }

        String id() {
            return klass + "#" + sig + "@" + line;
        }
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale, Lang lang) {
        Map<String, LinkedHashMap<String, Occ>> clumps = new LinkedHashMap<>();

        for (Ir.Klass k : project.classes) {
            for (Ir.Method m : k.methods) {
                List<Ir.Param> ps = m.params;
                if (ps.size() < 2) continue;
                if (isExcludedMethod(m)) continue;

                Occ occ = new Occ(k.qualifiedName, k.name, m.name,
                        k.filePath, m.signature(), m.line);

                for (int len = 2; len <= ps.size(); len++) {
                    for (int i = 0; i + len <= ps.size(); i++) {
                        List<Ir.Param> sub = ps.subList(i, i + len);
                        if (allSingleLetter(sub)) continue;              // C5
                        String key = sub.stream().map(Ir.Param::key)
                                .collect(Collectors.joining(", "));
                        clumps.computeIfAbsent(key, x -> new LinkedHashMap<>())
                                .put(occ.id(), occ);
                    }
                }
            }
        }

        List<Map.Entry<String, LinkedHashMap<String, Occ>>> candidates = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Occ>> e : clumps.entrySet()) {
            Collection<Occ> occs = e.getValue().values();
            if (occs.size() < 2) continue;                                // C4

            long classCount = occs.stream().map(o -> o.klass).distinct().count();
            if (classCount < 2) continue;                                 // C2 跨类

            long methodNameCount = occs.stream().map(o -> o.methodName).distinct().count();
            if (methodNameCount < 2) continue;                            // C3 非重载

            candidates.add(e);
        }

        candidates.sort((a, b) -> {
            int la = paramCount(a.getKey()), lb = paramCount(b.getKey());
            if (la != lb) return lb - la;
            return b.getValue().size() - a.getValue().size();
        });

        // 短参数团若其出现位置被更长的参数团完全覆盖，则为冗余
        List<Map.Entry<String, LinkedHashMap<String, Occ>>> accepted = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Occ>> c : candidates) {
            boolean subsumed = accepted.stream()
                    .anyMatch(a -> a.getValue().keySet().containsAll(c.getValue().keySet()));
            if (!subsumed) accepted.add(c);
        }

        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Occ>> e : accepted) {
            Collection<Occ> occs = e.getValue().values();
            int count = occs.size();

            List<String> classNames = occs.stream().map(o -> o.klassSimple)
                    .distinct().collect(Collectors.toList());
            List<String> methodNames = occs.stream().map(o -> o.methodName)
                    .distinct().collect(Collectors.toList());

            Finding.Severity sev = Finding.Severity.UNCONFIRMED;

            List<String> decls = Arrays.asList(e.getKey().split(", "));
            List<String> names = decls.stream()
                    .map(s -> s.substring(s.lastIndexOf(' ') + 1))
                    .collect(Collectors.toList());

            Finding f = new Finding(item(), sev, lang.pick(
                    String.format("(%s)  跨 %d 个类、%d 个方法，共出现 %d 次",
                            e.getKey(), classNames.size(), methodNames.size(), count),
                    String.format("(%s)  —  %d classes, %d methods, %d occurrences",
                            e.getKey(), classNames.size(), methodNames.size(), count)));
            f.weight = count * 10 + classNames.size();

            f.facts.put("paramDecls", decls);
            f.facts.put("paramNames", names);
            f.facts.put("classNames", classNames);
            f.facts.put("methodNames", methodNames);
            f.facts.put("occurrences", count);

            for (Occ o : occs) {
                f.locations.add(shortFile(o.file) + ":" + o.line + "   " + o.sig);
            }
            findings.add(f);
        }

        findings.sort((a, b) -> b.weight - a.weight);
        return findings;
    }

    private int paramCount(String key) {
        return key.split(", ").length;
    }

    private boolean allSingleLetter(List<Ir.Param> sub) {
        return sub.stream().allMatch(p -> p.name.length() == 1);
    }

    private boolean isExcludedMethod(Ir.Method m) {
        if (m.name.equals("main") && m.params.size() == 1) return true;
        return m.name.equals("equals") && m.params.size() == 1;
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
