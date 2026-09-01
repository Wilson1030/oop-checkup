package com.ooc.explain;

import com.ooc.report.Explanation;
import com.ooc.report.Finding;

import java.util.Collections;
import java.util.List;

/**
 * 默认解释器：带槽位的结构化模板。零依赖、离线、瞬时、完全可复现。
 *
 * 注意这不是「干巴巴的通用文案」—— 规则引擎已经把类名、方法名、参数名
 * 等事实全部提取到 Finding.facts 里，模板直接填槽，具体程度接近人工书写。
 *
 * 核心手法：以学生已有的 C 语言经验作为锚点。
 * 学生不是不懂「封装」的定义，而是不知道自己写的 Java 其实还是 C。
 * 先让他承认「我在 C 里本来就会这么做」，再指出 Java 只是多了一步。
 *
 * 排版约定：文本中只使用相对缩进（代码块缩进 4 格），
 * 基础缩进与折行由渲染层统一控制。
 */
public final class TemplateExplainer implements Explainer {

    @Override
    public Explanation explain(Finding f) {
        switch (f.item) {
            case PARAM_CLUMP:   return paramClump(f);
            case DATA_BEHAVIOR: return dataBehavior(f);
            default:            return new Explanation();
        }
    }

    // ---------------------------------------------------------------- 检查项 2

    private Explanation paramClump(Finding f) {
        List<String> decls   = f.fact("paramDecls",  Collections.<String>emptyList());
        List<String> names   = f.fact("paramNames",  Collections.<String>emptyList());
        List<String> classes = f.fact("classNames",  Collections.<String>emptyList());
        List<String> methods = f.fact("methodNames", Collections.<String>emptyList());
        int occ = f.fact("occurrences", 0);

        Explanation e = new Explanation();

        e.whatHappened = String.format(
                "(%s) 这 %d 个参数，在 %s 共 %d 个类的 %d 个方法里重复出现。",
                String.join(", ", decls), decls.size(),
                join(classes, 3), classes.size(), occ);

        e.cInstinct =
                "你在 C 里遇到这种情况会怎么写？\n" +
                "你多半会定义一个 struct，把这几个字段装进去，然后传它的指针。\n" +
                "因为你知道它们是一个整体 —— 拆开传又累，参数顺序还容易写反。\n" +
                "\n" +
                "那你为什么在 Java 里没这么做？\n" +
                "多半是因为「定义一个类」看起来比「定义一个 struct」重得多，\n" +
                "而你被教成「类是很严肃的东西，不能随便建」。这个观念是错的。";

        e.whyItMatters = String.format(
                "参数顺序写反时编译器往往不报错（类型相同），要到运行时才炸。\n" +
                "以后要给这组数据加一个字段，你得同时改 %d 处方法签名，漏一处就编译不过。",
                occ);

        StringBuilder sug = new StringBuilder();
        sug.append("把它们收进一个类：\n\n");
        sug.append("    class /* 你来起个名字 */ {\n");
        for (String d : decls) sug.append("        ").append(d).append(";\n");
        sug.append("    }\n\n");
        sug.append(String.format(
                "这 %d 个方法的参数列表会全部缩成 1 个。\n" +
                "起名这一步很重要 —— 如果起不出名字，说明它们可能确实不是一个概念。\n" +
                "\n" +
                "再往前一步，这才是「面向对象」的部分：\n" +
                "既然这个新类掌握了 %s 的全部信息，那么 %s 这样的方法\n" +
                "本来就该长在它身上，而不是留在外面。\n" +
                "C 的 struct 和 Java 的对象，唯一的区别就是：对象能带上操作自己的函数。",
                occ, join(names, 3), firstOr(methods, "相关操作")));
        e.suggestion = sug.toString();

        e.caveat =
                "如果这几个参数在业务上确实无关、只是碰巧同类型同名，那就不必封装。\n" +
                "判断标准只有一条：它们是不是同一件事的组成部分。";

        return e;
    }

    // ---------------------------------------------------------------- 检查项 1

    private Explanation dataBehavior(Finding f) {
        String cls   = f.fact("className", "该类");
        int fields   = f.fact("fieldCount", 0);
        int access   = f.fact("accessorCount", 0);
        int extCount = f.fact("externalAccess", 0);
        List<String> extClasses = f.fact("externalClasses", Collections.<String>emptyList());

        Explanation e = new Explanation();

        e.whatHappened = String.format(
                "%s 有 %d 个字段、%d 个 getter/setter，但没有任何业务方法 —— 它只是一个数据容器。%s",
                cls, fields, access,
                extCount > 0
                        ? String.format("\n项目里有 %d 个其他类、共 %d 处在读写它的数据：%s",
                                        extClasses.size(), extCount, join(extClasses, 4))
                        : "");

        e.cInstinct =
                "这正是 C 的写法：一个 struct 存数据，一组函数在外面操作它。\n" +
                "你把 struct 改名叫 class，把那些函数搬进了另一个类，\n" +
                "但结构一点没变 —— 数据在一边，操作数据的代码在另一边。\n" +
                "\n" +
                "在 C 里你别无选择，语言就只给了 struct。\n" +
                "在 Java 里你有选择，只是没用上。";

        e.whyItMatters = String.format(
                "%s 的字段含义，只有外面那些类知道。\n" +
                "哪天你改了某个字段的含义，编译器不会告诉你外部%s逻辑已经失效 ——\n" +
                "你只能靠自己记得。这正是 C 项目越写越难改的原因。",
                cls, extCount > 0 ? "那 " + extCount + " 处" : "的");

        e.suggestion = String.format(
                "找一个只用到 %s 自己字段的方法，把它搬进 %s。\n" +
                "搬完你会发现：它不再需要参数了 —— 因为数据就在手边。\n" +
                "\n" +
                "这就是「对象」这个词的全部含义：数据，加上操作这些数据的函数，\n" +
                "被放在同一个地方。不是「继承」，不是「多态」，就是这个。",
                cls, cls);

        e.caveat = String.format(
                "不是所有逻辑都该搬进来。涉及多个对象协作、或依赖外部资源\n" +
                "（数据库、网络、界面）的逻辑，留在 Service 里是对的。\n" +
                "另外，如果 %s 本来就是纯粹的数据传输对象，那贫血是合理设计。",
                cls);

        return e;
    }

    // ---------------------------------------------------------------- 工具

    private String join(List<String> list, int max) {
        if (list.isEmpty()) return "";
        if (list.size() <= max) return String.join("、", list);
        return String.join("、", list.subList(0, max)) + " 等";
    }

    private String firstOr(List<String> list, String def) {
        return list.isEmpty() ? def : list.get(0) + "()";
    }
}
