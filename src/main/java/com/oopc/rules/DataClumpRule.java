package com.oopc.rules;

import com.oopc.ir.Ir;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Item 2 - loose parameters avoided
 * Standard: Data Clump / Long Parameter List (Fowler, Refactoring - smells #3 #4)
 *
 * Criteria (v2; all must hold, see PREREGISTRATION-v2.md):
 *   C1 length >= 2 contiguous parameter subsequence, type and name both match
 *   C2 occurs in >= 2 different classes        <- new in v2
 *   C3 involves >= 2 different method names    <- new in v2
 *   C4 occurs >= 2 times
 *   C5 not all single-letter parameter names
 *
 * C2 / C3 are justifiable independently of any sample: a data clump's harm is
 * "the same data being split apart and passed across unrelated places". Helper
 * chains within one class sharing context, and overload families sharing a method
 * name, do not have that property -- they are normal or even good design.
 *
 * RUN-002 proved this rule's judgement is essentially **semantic**: the real
 * criterion is "can this parameter group be named as one meaningful concept",
 * which cannot be derived from syntax (the same syntax can be either).
 *
 * So this rule does not assert a violation; it only reports UNCONFIRMED, handing
 * the decision back to the student. That is not evasion -- pedagogically it is
 * better: the act of naming is itself forcing the student to decide whether these
 * things are one concept, which is object-oriented design.
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

        // A shorter clump whose locations are fully covered by a longer one is redundant.
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
