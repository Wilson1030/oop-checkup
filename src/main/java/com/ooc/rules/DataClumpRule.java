package com.ooc.rules;

import com.ooc.ir.Ir;
import com.ooc.report.Finding;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * R1 · 参数团（Data Clump）
 *
 * 若干参数总是结伴出现，说明它们本来就属于同一个概念，应当封装成对象。
 * 这是过程式思维最外显的痕迹：数据被拆散成一堆平铺的标量到处传。
 *
 * 阈值来自 PREREGISTRATION.md，已冻结：
 *   出现 >= dataClumpSevere 次 -> 严重
 *   出现 == 2 次              -> 中等
 */
public final class DataClumpRule implements Rule {

    @Override public String id()   { return "R1"; }
    @Override public String name() { return "参数团"; }

    private static final class Occ {
        final String klass, file, sig;
        final int line;
        Occ(String klass, String file, String sig, int line) {
            this.klass = klass; this.file = file; this.sig = sig; this.line = line;
        }
        String id() { return klass + "#" + sig + "@" + line; }
    }

    @Override
    public List<Finding> apply(Ir.Project project, ScaleProfile scale) {
        // key(参数序列) -> 出现位置（按 id 去重）
        Map<String, LinkedHashMap<String, Occ>> clumps = new LinkedHashMap<>();

        for (Ir.Klass k : project.classes) {
            for (Ir.Method m : k.methods) {
                List<Ir.Param> ps = m.params;
                if (ps.size() < 2) continue;
                if (isExcludedMethod(m)) continue;

                Occ occ = new Occ(k.qualifiedName, k.filePath, m.signature(), m.line);
                for (int len = 2; len <= ps.size(); len++) {
                    for (int i = 0; i + len <= ps.size(); i++) {
                        List<Ir.Param> sub = ps.subList(i, i + len);
                        if (allSingleLetter(sub)) continue;
                        String key = sub.stream().map(Ir.Param::key)
                                .collect(Collectors.joining(", "));
                        clumps.computeIfAbsent(key, x -> new LinkedHashMap<>())
                                .put(occ.id(), occ);
                    }
                }
            }
        }

        // 只保留出现 >= 2 次的
        List<Map.Entry<String, LinkedHashMap<String, Occ>>> candidates = clumps.entrySet().stream()
                .filter(e -> e.getValue().size() >= 2)
                .sorted((a, b) -> {
                    int la = paramCount(a.getKey()), lb = paramCount(b.getKey());
                    if (la != lb) return lb - la;                       // 参数多的优先
                    return b.getValue().size() - a.getValue().size();   // 出现次数多的优先
                })
                .collect(Collectors.toList());

        // 去重：若一个短参数团的出现位置被某个更长的参数团完全覆盖，则它是冗余的
        List<Map.Entry<String, LinkedHashMap<String, Occ>>> accepted = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Occ>> c : candidates) {
            boolean subsumed = accepted.stream()
                    .anyMatch(a -> a.getValue().keySet().containsAll(c.getValue().keySet()));
            if (!subsumed) accepted.add(c);
        }

        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<String, LinkedHashMap<String, Occ>> e : accepted) {
            int count = e.getValue().size();
            Finding.Severity sev = count >= scale.dataClumpSevere
                    ? Finding.Severity.RED : Finding.Severity.YELLOW;

            Finding f = new Finding(id(), name(), sev,
                    "(" + e.getKey() + ")  出现 " + count + " 次");
            f.weight = count;

            for (Occ o : e.getValue().values()) {
                f.locations.add(shortFile(o.file) + ":" + o.line + "   " + o.sig);
            }

            String names = Arrays.stream(e.getKey().split(", "))
                    .map(s -> s.substring(s.lastIndexOf(' ') + 1))
                    .collect(Collectors.joining("、"));
            int n = paramCount(e.getKey());

            f.whatHappened =
                    "这 " + n + " 个参数在 " + count + " 个方法里永远一起出现。"
                    + "当几个数据总是结伴而行，说明它们本来就是一个东西。";
            f.whyItMatters =
                    "以后要给这组数据加一个新字段，你得同时改 " + count + " 处方法签名，"
                    + "漏改一处就编译不过；而参数顺序写错时，编译器往往不会报错。";
            f.suggestion =
                    "把 " + names + " 封装成一个类，这 " + count + " 个方法的参数列表都会缩成 1 个。";
            f.caveat =
                    "如果这几个参数只是碰巧同名同类型、业务上毫无关系，那就不必封装。"
                    + "判断标准是：它们在概念上是不是同一件事的组成部分。";

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
        if (m.name.equals("equals") && m.params.size() == 1) return true;
        return false;
    }

    private String shortFile(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }
}
