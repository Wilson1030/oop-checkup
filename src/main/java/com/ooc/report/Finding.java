package com.ooc.report;

import java.util.ArrayList;
import java.util.List;

/** 一条检测发现 */
public final class Finding {

    public enum Severity {
        RED("[严重]"),
        YELLOW("[中等]"),
        GREEN("[通过]");

        public final String label;

        Severity(String label) {
            this.label = label;
        }
    }

    /** 规则编号，如 R1 */
    public final String ruleId;
    /** 规则名，如 参数团 */
    public final String ruleName;
    public final Severity severity;
    /** 一行摘要 */
    public final String title;
    /** 代码位置清单 */
    public final List<String> locations = new ArrayList<>();
    /** 发生了什么 */
    public String whatHappened = "";
    /** 为什么是问题 */
    public String whyItMatters = "";
    /** 试试 */
    public String suggestion = "";
    /** 但要注意 —— 防止学生矫枉过正 */
    public String caveat = "";
    /** 该发现涉及的重复次数 / 数量，用于排序与统计 */
    public int weight;

    public Finding(String ruleId, String ruleName, Severity severity, String title) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.severity = severity;
        this.title = title;
    }
}
