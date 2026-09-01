package com.ooc.explain;

import com.ooc.report.Explanation;
import com.ooc.report.Finding;

import java.util.Collections;
import java.util.List;

/**
 * 默认解释器：带槽位的结构化模板。零依赖、离线、瞬时、完全可复现。
 *
 * 核心手法：以学生已有的 C 语言经验作为锚点。
 * 学生不是不懂「封装」的定义，而是不知道自己写的 Java 其实还是 C。
 * 先让他承认「我在 C 里本来就是这么做的」，再指出 Java 只是多给了一个选择。
 *
 * 排版约定：文本只使用相对缩进（代码块缩进 4 格），
 * 基础缩进与折行由渲染层统一控制。
 */
public final class TemplateExplainer implements Explainer {

    @Override
    public Explanation explain(Finding f) {
        switch (f.item) {
            case PARAM_CLUMP:   return paramClump(f);
            case DATA_BEHAVIOR: return dataBehavior(f);
            case POLYMORPHISM:  return polymorphism(f);
            case STATIC_ABUSE:  return staticAbuse(f);
            case ENCAPSULATION: return encapsulation(f);
            case MAIN_BLOAT:    return mainBloat(f);
            default:            return new Explanation();
        }
    }

    // ------------------------------------------------ 检查项 1 · 数据与行为

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
                "被放在同一个地方。不是继承，不是多态，就是这个。",
                cls, cls);

        e.caveat = String.format(
                "不是所有逻辑都该搬进来。涉及多个对象协作、或依赖外部资源\n" +
                "（数据库、网络、界面）的逻辑，留在 Service 里是对的。\n" +
                "另外，如果 %s 本来就是纯粹的数据传输对象，那贫血是合理设计。",
                cls);
        return e;
    }

    // ------------------------------------------------ 检查项 2 · 散装参数（待确认）

    private Explanation paramClump(Finding f) {
        List<String> decls   = f.fact("paramDecls",  Collections.<String>emptyList());
        List<String> names   = f.fact("paramNames",  Collections.<String>emptyList());
        List<String> classes = f.fact("classNames",  Collections.<String>emptyList());
        int occ = f.fact("occurrences", 0);

        Explanation e = new Explanation();

        e.whatHappened = String.format(
                "(%s) 这 %d 个参数，在 %s 共 %d 个类的 %d 个方法里重复出现。",
                String.join(", ", decls), decls.size(),
                join(classes, 3), classes.size(), occ);

        e.cInstinct =
                "你在 C 里遇到这种情况会怎么写？\n" +
                "如果这几个东西是一个整体，你多半会定义一个 struct，然后传指针 ——\n" +
                "因为拆开传又累，参数顺序还容易写反。\n" +
                "\n" +
                "那你为什么在 Java 里没这么做？\n" +
                "多半是因为「定义一个类」看起来比「定义一个 struct」重得多，\n" +
                "而你被教成「类是很严肃的东西，不能随便建」。这个观念是错的。";

        e.whyItMatters = String.format(
                "如果它们确实是一个整体：参数顺序写反时编译器往往不报错（类型相同），\n" +
                "要到运行时才炸；以后加一个字段，你得同时改 %d 处方法签名。",
                occ);

        StringBuilder sug = new StringBuilder();
        sug.append("这一条需要你自己判断 —— 请回答一个问题：\n\n");
        sug.append(String.format("    你能给 %s 起一个名字吗？\n\n", join(names, 4)));
        sug.append("    起得出来  →  它们就该是一个类：\n\n");
        sug.append("        class /* 你起的那个名字 */ {\n");
        for (String d : decls) sug.append("            ").append(d).append(";\n");
        sug.append("        }\n\n");
        sug.append("    起不出来  →  忽略本条，它们只是碰巧同名。\n\n");
        sug.append(String.format(
                "如果起出来了，再往前一步 —— 这才是「面向对象」的部分：\n" +
                "既然这个新类掌握了全部信息，那么操作这些数据的方法\n" +
                "本来就该长在它身上，而不是留在那 %d 个地方。\n" +
                "C 的 struct 和 Java 的对象，唯一的区别就是：对象能带上操作自己的函数。",
                occ));
        e.suggestion = sug.toString();

        e.caveat =
                "本条为「待确认」而非「违反」，因为「这几个参数是不是同一个概念」\n" +
                "无法由代码结构判断，只能由写代码的人判断。\n" +
                "而做这个判断的过程，恰恰就是面向对象设计本身。";
        return e;
    }

    // ------------------------------------------------ 检查项 3 · 多态

    private Explanation polymorphism(Finding f) {
        List<String> types = f.fact("types", Collections.<String>emptyList());
        int repeat = f.fact("repeatCount", 1);
        String kind = f.fact("kind", "instanceof");
        boolean isSwitch = kind.equals("switch");

        Explanation e = new Explanation();

        e.whatHappened = String.format(
                "对 [%s] 的类型分派，%s。",
                join(types, 5),
                repeat >= 2 ? "在 " + repeat + " 处重复出现" : "使用了 " + kind + " 链");

        e.cInstinct =
                "在 C 里你只能这么做：struct 里放一个 int type 字段，\n" +
                "然后 switch(type) 分派到不同的处理函数。\n" +
                "这是 C 唯一能做「同一操作、不同实现」的手段。\n" +
                "\n" +
                "Java 给了你另一个手段，但你还在用 C 的那个。";

        e.whyItMatters = repeat >= 2
                ? String.format(
                        "每新增一种类型，你要同时修改这 %d 处。\n" +
                        "漏改一处，编译器不会报错 —— 要到运行时走到那个分支才炸。\n" +
                        "这就是 Fowler 把它列为坏味道的原因：改动是分散的。", repeat)
                : "每新增一种类型，这个判断链就要加一个分支。\n" +
                  "链越长，越容易漏掉某种情况，而编译器帮不上忙。";

        e.suggestion =
                "让每个类型自己实现同一个方法：\n\n" +
                "    interface Shape { double area(); }\n" +
                "    class Circle implements Shape { public double area() { … } }\n" +
                "    class Rect   implements Shape { public double area() { … } }\n\n" +
                "然后直接调用 shape.area()，由 Java 在运行时选择正确的实现。\n" +
                "新增一种类型时，你只加一个类，一处都不用改。\n" +
                "\n" +
                "这就是多态存在的全部理由 —— 它是 C 那个 switch 分派的替代品，\n" +
                "不是什么高深概念。";

        e.caveat =
                "如果分支逻辑很简单、而且类型确定不会再增加，保留 " +
                (isSwitch ? "switch" : "if-else") + " 是可以的。\n" +
                "多态的代价是逻辑分散到多个文件，看全貌变难了。\n" +
                "判断标准：这组类型未来还会不会增加？";
        return e;
    }

    // ------------------------------------------------ 检查项 4 · static

    private Explanation staticAbuse(Finding f) {
        String kind = f.fact("kind", "global-field");
        String cls = f.fact("className", "该类");
        Explanation e = new Explanation();

        if (kind.equals("global-field")) {
            List<String> fields = f.fact("fields", Collections.<String>emptyList());
            e.whatHappened = String.format(
                    "%s 里有 %d 个 static 且非 final 的字段：\n%s",
                    cls, fields.size(), "    " + String.join("\n    ", fields));

            e.cInstinct =
                    "这就是 C 的全局变量。\n" +
                    "你在 C 里写  int g_count;\n" +
                    "在 Java 里写  static int count;\n" +
                    "语义上是同一个东西 —— 一块所有代码都能读写的内存。";

            e.whyItMatters =
                    "任何一处代码都能改它，出问题时你无法知道是谁在什么时候改的。\n" +
                    "调试这类 bug 只能靠通读全部代码。\n" +
                    "如果程序有多线程，它还会直接导致数据竞争。";

            e.suggestion = String.format(
                    "问自己一个问题：这个状态属于谁？\n\n" +
                    "    属于某个具体的东西  →  变成那个对象的实例字段\n" +
                    "    属于整个程序        →  也应该有一个对象来持有它，\n" +
                    "                            并且只通过方法访问\n\n" +
                    "把 %s 的这些字段改成实例字段，然后把 %s 变成一个正常的对象。",
                    cls, cls);

            e.caveat =
                    "常量（static final）不在此列，那是正常且推荐的。\n" +
                    "真正需要全局唯一的资源（连接池、配置），可以用单例，\n" +
                    "但要把访问收敛到方法里，而不是直接暴露字段。";
        } else {
            int sc = f.fact("staticCount", 0);
            int tc = f.fact("totalCount", 0);
            int pct = f.fact("percent", 0);

            e.whatHappened = String.format(
                    "%s 的 %d 个方法里有 %d 个是 static（%d%%）。", cls, tc, sc, pct);

            e.cInstinct =
                    "你在写一组 C 函数，只是外面套了一个 class 的壳。\n" +
                    "这里的 class 只起了「命名空间」的作用，和 C 的头文件一样 ——\n" +
                    "它没有状态，不能被实例化出多个，也谈不上什么对象。";

            e.whyItMatters =
                    "static 方法之间只能靠参数传递数据。\n" +
                    "所以你会看到长长的参数列表在这些方法之间来回传 ——\n" +
                    "检查项 2 报出来的那些散装参数，根源往往就在这里。";

            e.suggestion = String.format(
                    "观察这些 static 方法反复操作的是哪几个数据。\n" +
                    "把那些数据变成 %s 的字段，把方法去掉 static 变成实例方法。\n" +
                    "\n" +
                    "做完你会发现参数列表大幅缩短 —— 因为数据已经在对象里了。", cls);

            e.caveat =
                    "纯工具类（数学计算、字符串处理这类无状态的）全 static 是正常的，\n" +
                    "本工具已按命名惯例自动排除。\n" +
                    "如果这个类确实是工具类，把它改名为 XxxUtils 即可。";
        }
        return e;
    }

    // ------------------------------------------------ 检查项 5 · 封装

    private Explanation encapsulation(Finding f) {
        String cls = f.fact("className", "该类");
        List<String> fields = f.fact("fields", Collections.<String>emptyList());

        Explanation e = new Explanation();

        e.whatHappened = String.format(
                "%s 有 %d 个 public 且可变的字段：\n%s",
                cls, fields.size(),
                "    " + String.join("\n    ", fields.size() > 6
                        ? fields.subList(0, 6) : fields)
                        + (fields.size() > 6 ? "\n    … 另有 " + (fields.size() - 6) + " 个" : ""));

        e.cInstinct =
                "C 的 struct 成员本来就是全公开的，你直接写 s.field = x 就行，\n" +
                "语言也没提供别的选择。\n" +
                "你把这个习惯原样带到了 Java —— 只是把 struct 换成了 class。";

        e.whyItMatters = String.format(
                "任何代码都能绕过 %s 的逻辑直接改它的字段。\n" +
                "这意味着：你想加一条规则（比如「数量不能是负数」）时，没有地方可以加 ——\n" +
                "因为根本没有一个必经的入口。", cls);

        e.suggestion = String.format(
                "先把这些字段改成 private，然后编译。\n" +
                "编译器报错的每一处，都是外部在直接操作 %s 的内部状态。\n" +
                "\n" +
                "对每一处报错问一句：\n" +
                "    「这个操作，是不是本来就该由 %s 自己提供一个方法来完成？」\n" +
                "\n" +
                "多数情况下答案是「是」。这个过程会自动把行为拉回到数据所在的地方 ——\n" +
                "顺便也就解决了检查项 1。", cls, cls);

        e.caveat =
                "public final 字段是安全的，不在此列。\n" +
                "纯数据传输对象（DTO/VO）可以不封装，本工具已按命名惯例排除。\n" +
                "另外，无脑给每个字段配一对 getter/setter 并不等于封装 ——\n" +
                "那只是把 public 换了种写法。";
        return e;
    }

    // ------------------------------------------------ 检查项 6 · main 肥胖

    private Explanation mainBloat(Finding f) {
        String cls = f.fact("className", "该类");
        int lines = f.fact("lines", 0);

        Explanation e = new Explanation();

        e.whatHappened = String.format("%s.main() 有 %d 行。", cls, lines);

        e.cInstinct =
                "这就是一个 C 程序的形状：一个 main，从头执行到尾。\n" +
                "在 C 里这很自然 —— 程序就是一条指令流。\n" +
                "你把这条指令流原样搬进了 Java 的 main 里。";

        e.whyItMatters = String.format(
                "%d 行的方法没法单独测试、没法复用、也很难读懂。\n" +
                "但更重要的是：它说明这个程序里还没有「对象」这个概念 ——\n" +
                "所有东西都排在同一条时间线上，没有任何东西是「有身份的」。", lines);

        e.suggestion =
                "在 main 里找一段连续的逻辑，问自己：\n\n" +
                "    「这一段在处理什么东西？」\n\n" +
                "那个「东西」就应该是一个类。\n" +
                "把这段逻辑，连同它反复操作的那几个变量，一起搬进那个类。\n" +
                "\n" +
                "重复几次之后，main 会缩短成几行 —— 只剩下创建对象、把它们接起来。\n" +
                "那才是 main 该做的事。";

        e.caveat =
                "如果这确实是一个一次性的脚本式小程序，main 长一点可以接受。\n" +
                "但超过 100 行时，里面通常已经藏着好几个不同的概念了。";
        return e;
    }

    // ------------------------------------------------ 工具

    private String join(List<String> list, int max) {
        if (list.isEmpty()) return "";
        if (list.size() <= max) return String.join("、", list);
        return String.join("、", list.subList(0, max)) + " 等 " + list.size() + " 项";
    }
}
