package com.oopc;

import com.oopc.explain.Explainer;
import com.oopc.explain.LlmConfig;
import com.oopc.explain.LlmExplainer;
import com.oopc.explain.TemplateExplainer;
import com.oopc.ir.Ir;
import com.oopc.parse.JavaFrontend;
import com.oopc.report.CheckItem;
import com.oopc.report.Finding;
import com.oopc.report.Lang;
import com.oopc.report.TextReporter;
import com.oopc.rules.AnemicModelRule;
import com.oopc.rules.DataClumpRule;
import com.oopc.rules.EncapsulationRule;
import com.oopc.rules.MainBloatRule;
import com.oopc.rules.PolymorphismRule;
import com.oopc.rules.Rule;
import com.oopc.rules.ScaleProfile;
import com.oopc.rules.StaticAbuseRule;

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
            new AnemicModelRule(),   // item 1
            new DataClumpRule(),     // item 2 (unconfirmed mode)
            new PolymorphismRule(),  // item 3
            new StaticAbuseRule(),   // item 4
            new EncapsulationRule(), // item 5
            new MainBloatRule()      // item 6
    );

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(
                new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

        if (args.length == 0) {
            printUsage(out);
            return;
        }

        Path root = Paths.get(args[0]);
        boolean summary = has(args, "--summary");
        boolean includeTests = has(args, "--include-tests");
        boolean batch = has(args, "--batch");
        int detail = intArg(args, "--detail", 3);
        Lang lang = Lang.parse(strArg(args, "--lang", "zh"));
        boolean noLlm = has(args, "--no-llm");
        String configPath = strArg(args, "--config", null);

        if (!Files.exists(root)) {
            out.println(lang.pick("路径不存在: ", "Path does not exist: ")
                    + root.toAbsolutePath());
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

        // Explanation layer.
        // Detection is always deterministic; only the prose may be LLM-enhanced,
        // and any failure silently falls back to the templates.
        Explainer templates = new TemplateExplainer(lang);
        LlmConfig llm = noLlm ? LlmConfig.disabled() : LlmConfig.load(configPath);
        Explainer explainer = llm.enabled
                ? new LlmExplainer(llm, lang, templates)
                : templates;

        if (!summary) {
            out.println(LlmExplainer.banner(llm, lang));
            out.println();
        }

        for (Path target : targets) {
            Ir.Project project = new JavaFrontend(includeTests).parse(target);
            ScaleProfile scale = ScaleProfile.of(project.effectiveLines);

            Map<CheckItem, List<Finding>> results = new EnumMap<>(CheckItem.class);
            for (Rule r : RULES) {
                List<Finding> fs = r.apply(project, scale, lang);
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
                new TextReporter(detail, lang).render(out, label, project, scale, results);
            }
        }
    }

    private static void printUsage(PrintStream out) {
        out.println("oop-checkup - OO Transition Checklist for Java");
        out.println();
        out.println("Usage: oop-checkup <path> [options]");
        out.println();
        out.println("Options:");
        out.println("  --lang zh|en       report language (default: zh)");
        out.println("  --detail N         expand at most N findings per item (default: 3)");
        out.println("  --include-tests    include test directories (excluded by default)");
        out.println("  --summary          one-line summary");
        out.println("  --batch            treat each subdirectory of <path> as a project");
        out.println("  --config <file>    LLM config file (default: ./oop-checkup.json)");
        out.println("  --no-llm           force built-in templates even if configured");
        out.println();
        out.println("The LLM is entirely optional. Without it the tool is offline,");
        out.println("free, deterministic and functionally complete.");
        out.println();
        out.println("Examples:");
        out.println("  oop-checkup examples/before");
        out.println("  oop-checkup examples/before --lang en --detail 20");
    }

    private static boolean has(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static String strArg(String[] args, String flag, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) return args[i + 1];
        }
        return def;
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
