package com.ooc;

import com.ooc.explain.Explainer;
import com.ooc.explain.TemplateExplainer;
import com.ooc.ir.Ir;
import com.ooc.parse.JavaFrontend;
import com.ooc.report.CheckItem;
import com.ooc.report.Finding;
import com.ooc.report.TextReporter;
import com.ooc.rules.AnemicModelRule;
import com.ooc.rules.DataClumpRule;
import com.ooc.rules.EncapsulationRule;
import com.ooc.rules.MainBloatRule;
import com.ooc.rules.PolymorphismRule;
import com.ooc.rules.Rule;
import com.ooc.rules.ScaleProfile;
import com.ooc.rules.StaticAbuseRule;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Main {

    private static final List<Rule> RULES = Arrays.asList(
            new AnemicModelRule(),   // 检查项 1
            new DataClumpRule(),     // 检查项 2（待确认）
            new PolymorphismRule(),  // 检查项 3
            new StaticAbuseRule(),   // 检查项 4
            new EncapsulationRule(), // 检查项 5
            new MainBloatRule()      // 检查项 6
    );

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

        if (args.length == 0) {
            out.println("oo-checkup —— 面向对象转换检查表");
            out.println();
            out.println("用法: oo-checkup <路径> [选项]");
            out.println();
            out.println("选项:");
            out.println("  --detail N         每个检查项最多展开 N 处违反（默认 3）");
            out.println("  --include-tests    包含测试目录（默认排除）");
            out.println("  --summary          一行式摘要（验证用，非产品功能）");
            out.println("  --batch            把 <路径> 下每个一级子目录各当作一个项目（验证用）");
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

        // 解释层：默认模板。未来接入 LlmExplainer 时在此替换，其余代码不动。
        Explainer explainer = new TemplateExplainer();

        for (Path target : targets) {
            Ir.Project project = new JavaFrontend(includeTests).parse(target);
            ScaleProfile scale = ScaleProfile.of(project.effectiveLines);

            Map<CheckItem, List<Finding>> results = new EnumMap<>(CheckItem.class);
            for (Rule r : RULES) {
                List<Finding> fs = r.apply(project, scale);
                for (Finding f : fs) {
                    f.explanation = explainer.explain(f);
                }
                results.put(r.item(), fs);
            }

            String label = target.getFileName() == null
                    ? target.toString() : target.getFileName().toString();

            if (summary) {
                out.println(TextReporter.summaryLine(label, project, scale, results));
            } else {
                new TextReporter(detail).render(out, label, project, scale, results);
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
