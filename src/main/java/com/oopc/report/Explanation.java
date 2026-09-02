package com.oopc.report;

/**
 * The explanation text for a single finding.
 *
 * Produced by an Explainer. The rule engine is only responsible for
 * "what / where / which standard"; the explanation layer is responsible for
 * "why / how to fix". They are strictly separated because the former must be
 * deterministic and reproducible, while the latter may be LLM-enhanced.
 */
public final class Explanation {

    /** What happened (objective statement). */
    public String whatHappened = "";
    /** How you would have written it in C - anchored on the reader's C experience. */
    public String cInstinct = "";
    /** Why it matters (consequences). */
    public String whyItMatters = "";
    /** Try this (an executable fix). */
    public String suggestion = "";
    /** But note (guards against overcorrection). */
    public String caveat = "";
}
