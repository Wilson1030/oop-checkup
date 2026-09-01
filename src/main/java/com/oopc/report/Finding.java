package com.oopc.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A single finding.
 *
 * "What / where / which standard / the facts" are produced deterministically
 * by the rule engine. An LLM must never add, remove or rewrite any of it —
 * otherwise it will hallucinate problems that do not exist and trust
 * collapses immediately. Only {@link #explanation} may be LLM-enhanced.
 */
public final class Finding {

    public enum Severity {
        RED("严重", "MAJOR"),
        YELLOW("中等", "MINOR"),
        /** Output of a semantic judgement: not asserted, handed back to the reader. */
        UNCONFIRMED("待确认", "UNCONFIRMED");

        private final String zh;
        private final String en;

        Severity(String zh, String en) {
            this.zh = zh;
            this.en = en;
        }

        public String label(Lang lang) {
            return lang.pick(zh, en);
        }

        public boolean isViolation() {
            return this != UNCONFIRMED;
        }
    }

    public final CheckItem item;
    public final Severity severity;
    /** One-line factual summary. */
    public final String title;
    /** Source locations. */
    public final List<String> locations = new ArrayList<>();
    /** Structured facts, consumed by the Explainer. */
    public final Map<String, Object> facts = new LinkedHashMap<>();
    /** Sort weight. */
    public int weight;

    /** Filled in by the Explainer; rule engine never writes here. */
    public Explanation explanation;

    public Finding(CheckItem item, Severity severity, String title) {
        this.item = item;
        this.severity = severity;
        this.title = title;
    }

    @SuppressWarnings("unchecked")
    public <T> T fact(String key, T def) {
        Object v = facts.get(key);
        return v == null ? def : (T) v;
    }
}
