package com.ooc.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一条检测发现。
 *
 * 「是什么 / 在哪 / 违反哪条标准 / 事实」由规则引擎确定性产出，
 * LLM 永远不得增删或改写这部分 —— 否则会幻觉出不存在的问题，信任立刻崩塌。
 * 只有 explanation 字段可以被 LLM 增强。
 */
public final class Finding {

    public enum Severity {
        RED("严重"),
        YELLOW("中等"),
        /** 语义类判据的输出：不断言违反，交由学生自己判断 */
        UNCONFIRMED("待确认");

        public final String label;

        Severity(String label) {
            this.label = label;
        }

        public boolean isViolation() {
            return this != UNCONFIRMED;
        }
    }

    public final CheckItem item;
    public final Severity severity;
    /** 一行摘要（客观事实） */
    public final String title;
    /** 代码位置清单 */
    public final List<String> locations = new ArrayList<>();
    /** 结构化事实，供 Explainer 填槽使用 */
    public final Map<String, Object> facts = new LinkedHashMap<>();
    /** 排序权重 */
    public int weight;

    /** 由 Explainer 填充，规则引擎不写这里 */
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
