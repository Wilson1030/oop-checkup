package com.ooc.rules;

/**
 * 规模自适应。
 *
 * 小作业和大项目必须用不同的尺子，否则会在小项目上疯狂误报，
 * 而误报一次就会永久失去用户信任。
 *
 * 阈值取自 PREREGISTRATION.md，在下载任何样本之前已冻结。
 */
public final class ScaleProfile {

    public enum Scale { TINY, NORMAL, LARGE }

    public final Scale scale;
    public final int effectiveLines;
    /** R1：参数团判为「严重」的出现次数下限 */
    public final int dataClumpSevere;
    /** R2：贫血模型判为「严重」的外部访问次数下限 */
    public final int anemicExternalSevere;
    /** 是否输出结论（TINY 规模不下结论） */
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
            // 50 行的作业全塞在 main 里是合理的，不下结论
            return new ScaleProfile(Scale.TINY, effectiveLines, 3, 10, false);
        }
        if (effectiveLines <= 500) {
            return new ScaleProfile(Scale.NORMAL, effectiveLines, 3, 10, true);
        }
        // 大项目按 1.5 倍放宽：3*1.5=4.5 -> 5，10*1.5 -> 15
        return new ScaleProfile(Scale.LARGE, effectiveLines, 5, 15, true);
    }

    public String describe() {
        switch (scale) {
            case TINY:   return "微型（< 80 有效行）— 不下结论";
            case NORMAL: return "常规（80-500 有效行）— 标准阈值";
            default:     return "大型（> 500 有效行）— 阈值 x1.5";
        }
    }
}
