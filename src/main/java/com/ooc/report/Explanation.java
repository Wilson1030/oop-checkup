package com.ooc.report;

/**
 * 一条发现的解释文本。
 *
 * 由 Explainer 生成 —— 规则引擎只负责「是什么、在哪、违反哪条标准」，
 * 解释层负责「为什么、怎么改」。两者严格分离，
 * 因为前者必须确定性可复现，后者可以由 LLM 增强。
 */
public final class Explanation {

    /** 发生了什么（客观陈述） */
    public String whatHappened = "";
    /** 你在 C 里会怎么写 —— 以学生已有的 C 经验作为锚点 */
    public String cInstinct = "";
    /** 为什么是问题（后果） */
    public String whyItMatters = "";
    /** 试试（可执行的改法） */
    public String suggestion = "";
    /** 但要注意（防止矫枉过正） */
    public String caveat = "";
}
