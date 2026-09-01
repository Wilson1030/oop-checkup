package com.ooc;

import com.ooc.ir.Ir;
import com.ooc.parse.JavaFrontend;
import com.ooc.report.Finding;
import com.ooc.report.TextReporter;
import com.ooc.rules.AnemicModelRule;
import com.ooc.rules.DataClumpRule;
import com.ooc.rules.Rule;
import com.ooc.rules.ScaleProfile;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Main {

    private static final List<Rule> RULES = Arrays.asList(
            new DataClumpRule(),
            new AnemicModelRule()
    );

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

        if (args.length == 0) {
            out.println("用法: oo-checkup <路径> [选项]");
            out.println();
            out.println("选项:");
            out.println("  --summary          只输出一行摘要（用于多样本对比）");
            out.println("  --include-tests    包含测试目录（默认排除）");
            out.println("  --detail N         每条规则最多展开 N 项（默认 3）");
            out.println("  --batch            把 <路径> 下的每个一级子目录各当作一个样本");
            return;
        }

        Path root = Paths.get(args[0]);
        boolean summary = has(args, "--summary");
        boolean includeTests = has(args, "--include-tests");
        boolean batch = has(args, "--batch");
        int detail = intArg(args, "--detail", 3);

        if (!Files.exists(root)) {
            out.println("路径不存在: " + root.toAbsolutePath());
            return;
        }

        List<Path> targets = new ArrayList<>();
        if (batch) {
            try (var s = Files.list(root)) {
                s.filter(Files::isDirectory).sorted().forEach(targets::add);
            }
        } else {
            targets.add(root);
        }

        if (summary) {
            out.println("样本                                             有效行    类  |  R1 参数团  |  R2 贫血模型");
            out.println("---------------------------------------------------------------------------------------------");
        }

        for (Path target : targets) {
            Ir.Project project = new JavaFrontend(includeTests).parse(target);
            ScaleProfile scale = ScaleProfile.of(project.effectiveLines);

            Map<String, List<Finding>> byRule = new LinkedHashMap<>();
            for (Rule r : RULES) {
                byRule.put(r.id() + " " + r.name(), r.apply(project, scale));
            }

            String label = target.getFileName() == null
                    ? target.toString() : target.getFileName().toString();

            if (summary) {
                out.println(TextReporter.summaryLine(label, project, scale, byRule));
            } else {
                new TextReporter(detail).render(out, label, project, scale, byRule);
            }
        }
    }

    private static boolean has(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static int intArg(String[] args, String flag, int def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                try { return Integer.parseInt(args[i + 1]); } catch (Exception ignored) { }
            }
        }
        return def;
    }
}
