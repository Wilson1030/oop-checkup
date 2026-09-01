package com.ooc.rules;

import com.ooc.report.Lang;

/**
 * Scale adaptation.
 *
 * Small assignments and large projects need different yardsticks. Without
 * this, the tool would fire constantly on tiny programs — and a single false
 * positive permanently destroys the reader's trust.
 *
 * Thresholds come from PREREGISTRATION.md, frozen before any sample was seen.
 */
public final class ScaleProfile {

    public enum Scale { TINY, NORMAL, LARGE }

    public final Scale scale;
    public final int effectiveLines;
    /** Item 2: occurrence count at which a data clump becomes MAJOR. */
    public final int dataClumpSevere;
    /** Item 1: external-access count at which an anemic class becomes MAJOR. */
    public final int anemicExternalSevere;
    /** Whether conclusions are reported at all (TINY draws no conclusions). */
    public final boolean conclusive;

    private ScaleProfile(Scale scale, int lines, int dcSevere, int anSevere, boolean conclusive) {
        this.scale = scale;
        this.effectiveLines = lines;
        this.dataClumpSevere = dcSevere;
        this.anemicExternalSevere = anSevere;
        this.conclusive = conclusive;
    }

    public static ScaleProfile of(int effectiveLines) {
        if (effectiveLines < 80) {
            // A 50-line program with everything in main is perfectly reasonable.
            return new ScaleProfile(Scale.TINY, effectiveLines, 3, 10, false);
        }
        if (effectiveLines <= 500) {
            return new ScaleProfile(Scale.NORMAL, effectiveLines, 3, 10, true);
        }
        return new ScaleProfile(Scale.LARGE, effectiveLines, 5, 15, true);
    }

    public String describe(Lang lang) {
        switch (scale) {
            case TINY:
                return lang.pick("微型（< 80 有效行）— 不下结论",
                                 "tiny (< 80 effective lines) — no conclusions drawn");
            case NORMAL:
                return lang.pick("常规（80-500 有效行）— 标准阈值",
                                 "normal (80-500 effective lines) — standard thresholds");
            default:
                return lang.pick("大型（> 500 有效行）— 阈值 x1.5",
                                 "large (> 500 effective lines) — thresholds x1.5");
        }
    }
}
