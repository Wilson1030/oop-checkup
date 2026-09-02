package com.oopc.explain;

import com.oopc.report.Explanation;
import com.oopc.report.Finding;

/**
 * The explanation layer.
 *
 * Hard boundary:
 *   An Explainer may only write prose for a finding that the rule engine has
 *   already determined. It must never add, remove or modify any judgement.
 *
 * Implementations:
 *   TemplateExplainer - default; slot-filled structured templates;
 *                       zero deps, offline, instant
 *   LlmExplainer      - optional; bring-your-own API key (BYOK); no effect on
 *                       functionality when not configured
 *
 * Any implementation failure must return null so the caller can fall back to
 * the template. The report must always be produced.
 */
public interface Explainer {

    Explanation explain(Finding finding);

    /** Called when the student asks "I still don't get it". Template returns null (unsupported). */
    default String followUp(Finding finding, String question) {
        return null;
    }
}
