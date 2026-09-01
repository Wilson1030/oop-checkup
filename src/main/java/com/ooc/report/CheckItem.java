package com.ooc.report;

/**
 * 面向对象转换检查表 —— 产品的骨架。
 *
 * 每一项都对应一条公认标准，并注明出处。
 * 这是本工具与「评分制」的根本区别：
 *   评分制隐含存在一把普适的尺子，而那把尺子只在「具体项目该怎么设计」
 *   这一层才需要，恰恰是没有标准的一层。
 *   检查表只断言「语言语义 / 设计原则 / 坏味道清单」这三层，它们有公认标准。
 *
 * semantic = true 的检查项，其判据本质上依赖语义理解（「这组参数是不是
 * 一个概念」「这个 String 该不该有类型」），纯静态分析无法达到可用准确率。
 * 这类检查项只输出「待确认」，把判断权交还给学生 —— 教学上反而更好，
 * 因为做这个判断的过程正是我们希望他学会的思考。
 */
public enum CheckItem {

    DATA_BEHAVIOR("1", "数据与行为是否结合",
            "Anemic Domain Model",
            "Martin Fowler, AnemicDomainModel, 2003",
            true, false),

    PARAM_CLUMP("2", "是否避免散装参数传递",
            "Data Clump / Long Parameter List",
            "Fowler《重构》坏味道 #3 #4",
            true, true),

    POLYMORPHISM("3", "是否用多态替代类型判断",
            "Switch Statements",
            "Fowler《重构》坏味道 #11",
            true, false),

    STATIC_ABUSE("4", "static 是否被滥用",
            "全局状态破坏封装",
            "Java 教科书通识",
            true, false),

    ENCAPSULATION("5", "封装是否完整",
            "public 可变字段暴露内部表示",
            "Fowler《重构》坏味道 #5 关联",
            true, false),

    MAIN_BLOAT("6", "入口方法是否过度承担",
            "Long Method（main 特化）",
            "Fowler《重构》坏味道 #6",
            true, false),

    PRIMITIVE_OBSESSION("7", "是否避免基本类型偏执",
            "Primitive Obsession",
            "Fowler《重构》坏味道 #2",
            false, true);

    public final String no;
    public final String title;
    /** 违反的标准名 */
    public final String standard;
    /** 文献出处 */
    public final String source;
    /** 是否已实现 */
    public final boolean implemented;
    /** 判据是否本质依赖语义理解 —— 此类只输出「待确认」，不断言违反 */
    public final boolean semantic;

    CheckItem(String no, String title, String standard, String source,
              boolean implemented, boolean semantic) {
        this.no = no;
        this.title = title;
        this.standard = standard;
        this.source = source;
        this.implemented = implemented;
        this.semantic = semantic;
    }
}
