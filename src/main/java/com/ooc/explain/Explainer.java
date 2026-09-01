package com.ooc.explain;

import com.ooc.report.Explanation;
import com.ooc.report.Finding;

/**
 * 解释层接口。
 *
 * 边界（死线）：
 *   Explainer 只能为「已经由规则引擎确定的发现」生成说明文字，
 *   绝不能新增、撤销或修改任何一条判定。
 *
 * 实现：
 *   TemplateExplainer —— 默认，带槽位的结构化模板，零依赖、离线、瞬时
 *   LlmExplainer      —— 可选，用户自带 API（BYOK），未配置时不影响任何功能
 *
 * 任何实现失败都必须返回 null，由调用方降级到模板，绝不阻断报告输出。
 */
public interface Explainer {

    Explanation explain(Finding finding);

    /** 学生追问「我还是不懂」时调用。模板实现返回 null 表示不支持。 */
    default String followUp(Finding finding, String question) {
        return null;
    }
}
