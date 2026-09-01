package com.oopc.explain;

import com.oopc.report.Explanation;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Optional LLM-backed explainer, OpenAI-compatible, bring-your-own-key.
 *
 * HARD BOUNDARY
 * -------------
 * This class may only rewrite the five prose sections of an already-decided
 * finding. It never adds, removes or alters a judgement. The rule engine has
 * already determined *what* was found, *where*, and *which standard* applies;
 * an LLM allowed to touch that would hallucinate problems that do not exist,
 * and a single false positive destroys the reader's trust permanently.
 *
 * FAILURE POLICY
 * --------------
 * Every failure path — no config, timeout, HTTP error, malformed response,
 * missing section — silently falls back to {@link TemplateExplainer}.
 * The report must always be produced.
 *
 * WHAT IS SENT
 * ------------
 * Only the finding metadata and the code fragments that already appear in the
 * report (class names, method signatures, field declarations, line numbers).
 * Whole source files are never transmitted.
 */
public final class LlmExplainer implements Explainer {

    private static final String WHAT = "###WHAT###";
    private static final String CSEC = "###C###";
    private static final String WHY  = "###WHY###";
    private static final String TRY  = "###TRY###";
    private static final String NOTE = "###NOTE###";

    private final LlmConfig cfg;
    private final Lang lang;
    private final Explainer fallback;
    private final HttpClient http;

    private int failures = 0;
    private static final int MAX_FAILURES = 3;

    public LlmExplainer(LlmConfig cfg, Lang lang, Explainer fallback) {
        this.cfg = cfg;
        this.lang = lang;
        this.fallback = fallback;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(cfg.timeoutMs, 10_000)))
                .build();
    }

    @Override
    public Explanation explain(Finding finding) {
        Explanation base = fallback.explain(finding);
        if (!cfg.enabled || failures >= MAX_FAILURES) return base;

        try {
            String body = call(buildPrompt(finding));
            if (body == null) return giveUp(base);
            Explanation e = parse(body);
            return e == null ? giveUp(base) : e;
        } catch (Exception ex) {
            return giveUp(base);
        }
    }

    /** After repeated failures stop trying: do not stall the whole report. */
    private Explanation giveUp(Explanation base) {
        failures++;
        return base;
    }

    // ------------------------------------------------------------------ prompt

    private String buildPrompt(Finding f) {
        StringBuilder sb = new StringBuilder();

        sb.append(lang.isEn()
                ? "You are helping a student who learned C and is now writing Java.\n"
                : "你在帮一个学完 C、正在写 Java 的学生。\n");

        sb.append(lang.isEn()
                ? "A static analyser has ALREADY decided this finding. Do not question it, "
                + "do not add new problems, do not mention anything not listed below.\n"
                + "Your only job is to explain this specific finding well.\n\n"
                : "静态分析器已经判定了下面这条发现。不要质疑它，不要新增问题，"
                + "不要提及下面没有列出的任何东西。\n你唯一的任务是把这一条讲清楚。\n\n");

        sb.append("FINDING\n");
        sb.append("  checklist item : ").append(f.item.no)
          .append(" - ").append(f.item.title(lang)).append('\n');
        sb.append("  standard       : ").append(f.item.standard(lang)).append('\n');
        sb.append("  source         : ").append(f.item.source).append('\n');
        sb.append("  summary        : ").append(f.title).append('\n');
        if (!f.locations.isEmpty()) {
            sb.append("  locations      :\n");
            for (String loc : f.locations) sb.append("      ").append(loc).append('\n');
        }
        if (!f.facts.isEmpty()) {
            sb.append("  facts          :\n");
            for (Map.Entry<String, Object> e : f.facts.entrySet()) {
                sb.append("      ").append(e.getKey()).append(" = ")
                  .append(String.valueOf(e.getValue())).append('\n');
            }
        }

        sb.append('\n');
        sb.append(lang.isEn() ? """
                Write exactly five sections, using these markers on their own lines:

                ###WHAT###   State the facts. Reference the real class and method names above.
                ###C###      How the student would have written this in C, and why they had
                             no choice there but do have one in Java. This is the key section:
                             the student can already recite the definition of encapsulation;
                             what they do not realise is that their Java is still C.
                ###WHY###    Concrete consequences. No lecturing, no generic principles.
                ###TRY###    One executable step they can take right now.
                ###NOTE###   Guard against overcorrection: when this pattern is actually fine.

                Rules: plain text only, no markdown headings or bullets. Keep every line under
                75 characters. Be concrete and use the real identifiers. Do not invent facts.
                """ : """
                请写恰好五段，用下面这些标记独占一行分隔：

                ###WHAT###   陈述事实，引用上面真实的类名和方法名。
                ###C###      这个学生在 C 里会怎么写，以及为什么 C 里别无选择而 Java 里有选择。
                             这是最关键的一段：学生不是不懂封装的定义，
                             是不知道自己写的 Java 其实还是 C。
                ###WHY###    具体后果。不要讲大道理，不要泛泛而谈原则。
                ###TRY###    一个他现在就能执行的动作。
                ###NOTE###   防止矫枉过正：什么情况下这样写其实是对的。

                要求：纯文本，不要 markdown 标题或列表。每行不超过 34 个汉字。
                要具体，要用真实标识符。不要编造事实。
                """);

        return sb.toString();
    }

    // ------------------------------------------------------------------ http

    private String call(String prompt) throws Exception {
        String payload = "{"
                + "\"model\":\"" + Json.escape(cfg.model) + "\","
                + "\"temperature\":0.2,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\""
                + Json.escape(prompt) + "\"}]"
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(cfg.baseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(cfg.timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + cfg.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) return null;

        String content = extractContent(resp.body());
        return (content == null || content.isBlank()) ? null : content;
    }

    /** Pull choices[0].message.content out of an OpenAI-compatible response. */
    private String extractContent(String body) {
        int msg = body.indexOf("\"message\"");
        String scope = msg >= 0 ? body.substring(msg) : body;
        return Json.stringField(scope, "content");
    }

    // ------------------------------------------------------------------ parse

    private Explanation parse(String text) {
        if (!text.contains(WHAT) || !text.contains(CSEC) || !text.contains(WHY)
                || !text.contains(TRY) || !text.contains(NOTE)) {
            return null;
        }
        Explanation e = new Explanation();
        e.whatHappened = section(text, WHAT, CSEC);
        e.cInstinct    = section(text, CSEC, WHY);
        e.whyItMatters = section(text, WHY,  TRY);
        e.suggestion   = section(text, TRY,  NOTE);
        e.caveat       = section(text, NOTE, null);

        if (isBlank(e.whatHappened) || isBlank(e.cInstinct) || isBlank(e.whyItMatters)
                || isBlank(e.suggestion) || isBlank(e.caveat)) {
            return null;
        }
        return e;
    }

    private String section(String text, String from, String to) {
        int a = text.indexOf(from);
        if (a < 0) return "";
        a += from.length();
        int b = (to == null) ? text.length() : text.indexOf(to, a);
        if (b < 0) b = text.length();
        return text.substring(a, b).strip();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Unused placeholder kept for interface symmetry; see Explainer#followUp. */
    @Override
    public String followUp(Finding finding, String question) {
        return null;
    }

    /** Diagnostic banner printed once at startup. */
    public static String banner(LlmConfig cfg, Lang lang) {
        if (!cfg.enabled) {
            return lang.pick(
                    "解释层：内置模板（未配置 LLM，功能完整）",
                    "Explanations: built-in templates (no LLM configured; fully functional)");
        }
        return lang.pick(
                "解释层：LLM " + cfg.model + " via " + cfg.baseUrl + "（失败自动降级为模板）",
                "Explanations: LLM " + cfg.model + " via " + cfg.baseUrl
                        + " (falls back to templates on any failure)");
    }

    /** Never log the key. */
    public static List<String> redactedKeys() {
        return List.of("apiKey", "Authorization");
    }
}
