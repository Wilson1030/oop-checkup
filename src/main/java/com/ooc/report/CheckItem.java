package com.ooc.report;

/**
 * The checklist — the backbone of this tool.
 *
 * Every item maps to a recognised standard with a citable source.
 * This is what separates a checklist from a score: a score implies a
 * universal yardstick, which only exists at the "how should this project
 * be designed" layer — precisely the layer that has no standard.
 * The checklist only asserts things that do have standards.
 *
 * semantic = true means the judgement fundamentally requires understanding
 * meaning ("are these parameters one concept?"), which static analysis
 * cannot do reliably. Such items only emit "unconfirmed" and hand the
 * decision back to the reader — which is pedagogically better anyway.
 */
public enum CheckItem {

    DATA_BEHAVIOR("1",
            "数据与行为是否结合", "Data and behaviour kept together",
            "Anemic Domain Model", "Anemic Domain Model",
            "Martin Fowler, AnemicDomainModel, 2003",
            true, false),

    PARAM_CLUMP("2",
            "是否避免散装参数传递", "Loose parameters avoided",
            "Data Clump / Long Parameter List", "Data Clump / Long Parameter List",
            "Fowler, Refactoring — smells #3 #4",
            true, true),

    POLYMORPHISM("3",
            "是否用多态替代类型判断", "Polymorphism instead of type checks",
            "Switch Statements", "Switch Statements",
            "Fowler, Refactoring — smell #11",
            true, false),

    STATIC_ABUSE("4",
            "static 是否被滥用", "static not abused",
            "全局状态破坏封装", "Global mutable state breaks encapsulation",
            "Standard Java teaching material",
            true, false),

    ENCAPSULATION("5",
            "封装是否完整", "Encapsulation intact",
            "public 可变字段暴露内部表示", "public mutable fields expose internals",
            "Fowler, Refactoring — related to smell #5",
            true, false),

    MAIN_BLOAT("6",
            "入口方法是否过度承担", "Entry point not overloaded",
            "Long Method（main 特化）", "Long Method (main-specific)",
            "Fowler, Refactoring — smell #6",
            true, false),

    PRIMITIVE_OBSESSION("7",
            "是否避免基本类型偏执", "Primitive obsession avoided",
            "Primitive Obsession", "Primitive Obsession",
            "Fowler, Refactoring — smell #2",
            false, true);

    public final String no;
    private final String titleZh;
    private final String titleEn;
    private final String standardZh;
    private final String standardEn;
    public final String source;
    /** Whether the rule is implemented. */
    public final boolean implemented;
    /** Whether the judgement fundamentally needs semantic understanding. */
    public final boolean semantic;

    CheckItem(String no, String titleZh, String titleEn,
              String standardZh, String standardEn, String source,
              boolean implemented, boolean semantic) {
        this.no = no;
        this.titleZh = titleZh;
        this.titleEn = titleEn;
        this.standardZh = standardZh;
        this.standardEn = standardEn;
        this.source = source;
        this.implemented = implemented;
        this.semantic = semantic;
    }

    public String title(Lang lang) {
        return lang.pick(titleZh, titleEn);
    }

    public String standard(Lang lang) {
        return lang.pick(standardZh, standardEn);
    }
}
