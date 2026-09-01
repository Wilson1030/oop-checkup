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
 * 学生质疑时可以直接回到出处，这是教学工具的信任地基。
 */
public enum CheckItem {

    DATA_BEHAVIOR("1", "数据与行为是否结合",
            "Anemic Domain Model",
            "Martin Fowler, AnemicDomainModel, 2003",
            true),

    PARAM_CLUMP("2", "是否避免散装参数传递",
            "Data Clump / Long Parameter List",
            "Fowler《重构》坏味道 #3 #4",
            true),

    POLYMORPHISM("3", "是否用多态替代类型判断",
            "Switch Statements",
            "Fowler《重构》坏味道 #11",
            false),

    STATIC_ABUSE("4", "static 是否被滥用",
            "全局状态破坏封装",
            "Java 教科书通识",
            false),

    ENCAPSULATION("5", "封装是否完整",
            "Feature Envy / public 可变字段",
            "Fowler《重构》坏味道 #5",
            false),

    GRANULARITY("6", "类与方法的粒度",
            "Large Class / Long Method",
            "Fowler《重构》坏味道 #1 #6",
            false),

    PRIMITIVE_OBSESSION("7", "是否避免基本类型偏执",
            "Primitive Obsession",
            "Fowler《重构》坏味道 #2",
            false);

    public final String no;
    public final String title;
    /** 违反的标准名 */
    public final String standard;
    /** 文献出处 */
    public final String source;
    /** 是否已实现 */
    public final boolean implemented;

    CheckItem(String no, String title, String standard, String source, boolean implemented) {
        this.no = no;
        this.title = title;
        this.standard = standard;
        this.source = source;
        this.implemented = implemented;
    }
}
