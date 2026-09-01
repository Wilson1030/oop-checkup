package com.oopc.explain;

import com.oopc.report.Explanation;
import com.oopc.report.Finding;
import com.oopc.report.Lang;

import java.util.Collections;
import java.util.List;

/**
 * Default explainer: slot-filled structured templates.
 * Zero dependencies, offline, instant, fully reproducible.
 *
 * These are not generic boilerplate — the rule engine has already extracted
 * class names, method names and parameter names into Finding.facts, so the
 * templates fill in real specifics.
 *
 * Core technique: anchor everything to the reader's existing C experience.
 * A student who writes C-flavoured Java usually *can* recite the definition
 * of encapsulation; what they don't realise is that their Java is still C.
 * So first get them to admit "yes, in C I would have done exactly that",
 * then show that Java merely offers one more option.
 *
 * Layout: text uses relative indentation only (code blocks indented 4).
 * Base indentation and wrapping are handled by the renderer.
 */
public final class TemplateExplainer implements Explainer {

    private final Lang lang;

    public TemplateExplainer(Lang lang) {
        this.lang = lang;
    }

    @Override
    public Explanation explain(Finding f) {
        switch (f.item) {
            case DATA_BEHAVIOR: return lang.isEn() ? dataBehaviorEn(f) : dataBehaviorZh(f);
            case PARAM_CLUMP:   return lang.isEn() ? paramClumpEn(f)   : paramClumpZh(f);
            case POLYMORPHISM:  return lang.isEn() ? polymorphismEn(f) : polymorphismZh(f);
            case STATIC_ABUSE:  return lang.isEn() ? staticAbuseEn(f)  : staticAbuseZh(f);
            case ENCAPSULATION: return lang.isEn() ? encapsulationEn(f): encapsulationZh(f);
            case MAIN_BLOAT:    return lang.isEn() ? mainBloatEn(f)    : mainBloatZh(f);
            default:            return new Explanation();
        }
    }

    // ================================================== Item 1 · data & behaviour

    private Explanation dataBehaviorZh(Finding f) {
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

    private Explanation dataBehaviorEn(Finding f) {
        String cls   = f.fact("className", "this class");
        int fields   = f.fact("fieldCount", 0);
        int access   = f.fact("accessorCount", 0);
        int extCount = f.fact("externalAccess", 0);
        List<String> extClasses = f.fact("externalClasses", Collections.<String>emptyList());

        Explanation e = new Explanation();
        e.whatHappened = String.format(
                "%s has %d fields and %d getters/setters, but not a single business method — "
                + "it is just a data container.%s",
                cls, fields, access,
                extCount > 0
                        ? String.format("\n%d other classes read or write its data, %d times in total: %s",
                                        extClasses.size(), extCount, join(extClasses, 4))
                        : "");
        e.cInstinct =
                "This is exactly the C shape: a struct holds the data, and a set of\n" +
                "functions outside operates on it.\n" +
                "You renamed the struct to a class and moved those functions into\n" +
                "another class — but the structure never changed. Data on one side,\n" +
                "the code that manipulates it on the other.\n" +
                "\n" +
                "In C you had no choice; the language only gave you struct.\n" +
                "In Java you do have a choice. You just didn't take it.";
        e.whyItMatters = String.format(
                "The meaning of %s's fields lives only in those outside classes.\n" +
                "The day you change what a field means, the compiler will not tell you\n" +
                "which of %s has silently become wrong — you have to remember it yourself.\n" +
                "This is precisely why C projects get harder to change over time.",
                cls, extCount > 0 ? "those " + extCount + " places" : "the outside logic");
        e.suggestion = String.format(
                "Find one method that only touches %s's own fields and move it into %s.\n" +
                "Once you do, you will notice it no longer needs parameters — the data\n" +
                "is already right there.\n" +
                "\n" +
                "That is the whole meaning of the word \"object\": data, plus the functions\n" +
                "that operate on that data, living in the same place.\n" +
                "Not inheritance. Not polymorphism. Just this.",
                cls, cls);
        e.caveat = String.format(
                "Not every method belongs inside. Logic that coordinates several objects,\n" +
                "or depends on external resources (database, network, UI), rightly stays\n" +
                "in a service class.\n" +
                "Also, if %s genuinely is a pure data transfer object, being anemic is a\n" +
                "deliberate and correct design.",
                cls);
        return e;
    }

    // ================================================== Item 2 · data clump (unconfirmed)

    private Explanation paramClumpZh(Finding f) {
        List<String> decls   = f.fact("paramDecls",  Collections.<String>emptyList());
        List<String> names   = f.fact("paramNames",  Collections.<String>emptyList());
        List<String> classes = f.fact("classNames",  Collections.<String>emptyList());
        int occ = f.fact("occurrences", 0);

        Explanation e = new Explanation();
        e.whatHappened = String.format(
                "(%s) 这 %d 个参数，在 %s 共 %d 个类的 %d 个方法里重复出现。",
                String.join(", ", decls), decls.size(), join(classes, 3), classes.size(), occ);
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
                "要到运行时才炸；以后加一个字段，你得同时改 %d 处方法签名。", occ);
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
                "C 的 struct 和 Java 的对象，唯一的区别就是：对象能带上操作自己的函数。", occ));
        e.suggestion = sug.toString();
        e.caveat =
                "本条为「待确认」而非「违反」，因为「这几个参数是不是同一个概念」\n" +
                "无法由代码结构判断，只能由写代码的人判断。\n" +
                "而做这个判断的过程，恰恰就是面向对象设计本身。";
        return e;
    }

    private Explanation paramClumpEn(Finding f) {
        List<String> decls   = f.fact("paramDecls",  Collections.<String>emptyList());
        List<String> names   = f.fact("paramNames",  Collections.<String>emptyList());
        List<String> classes = f.fact("classNames",  Collections.<String>emptyList());
        int occ = f.fact("occurrences", 0);

        Explanation e = new Explanation();
        e.whatHappened = String.format(
                "(%s) — these %d parameters travel together through %d methods across %d classes (%s).",
                String.join(", ", decls), decls.size(), occ, classes.size(), join(classes, 3));
        e.cInstinct =
                "How would you have written this in C?\n" +
                "If these things belong together, you would almost certainly have declared\n" +
                "a struct and passed a pointer — because passing them apart is tedious and\n" +
                "it is far too easy to get the argument order wrong.\n" +
                "\n" +
                "So why didn't you do that in Java?\n" +
                "Most likely because \"declaring a class\" feels much heavier than \"declaring\n" +
                "a struct\", and you were taught that classes are serious things you should\n" +
                "not create casually. That belief is wrong.";
        e.whyItMatters = String.format(
                "If they really are one thing: swapping two arguments of the same type\n" +
                "compiles perfectly and only explodes at runtime; and adding one more\n" +
                "field means editing %d method signatures at once.", occ);
        StringBuilder sug = new StringBuilder();
        sug.append("This one is your call. Answer a single question:\n\n");
        sug.append(String.format("    Can you give %s a name?\n\n", join(names, 4)));
        sug.append("    Yes  ->  then they should be a class:\n\n");
        sug.append("        class /* the name you just came up with */ {\n");
        for (String d : decls) sug.append("            ").append(d).append(";\n");
        sug.append("        }\n\n");
        sug.append("    No   ->  ignore this finding; they just happen to appear together.\n\n");
        sug.append(String.format(
                "If you did find a name, go one step further — this is the part that is\n" +
                "actually object-oriented:\n" +
                "now that the new class holds all the information, the methods operating on\n" +
                "that data belong on it, not scattered across those %d places.\n" +
                "The only difference between a C struct and a Java object is that an object\n" +
                "can carry the functions that operate on itself.", occ));
        e.suggestion = sug.toString();
        e.caveat =
                "This is reported as UNCONFIRMED rather than a violation, because whether\n" +
                "these parameters form one concept cannot be derived from code structure.\n" +
                "Only the person writing the code can decide.\n" +
                "And making that decision is object-oriented design itself.";
        return e;
    }

    // ================================================== Item 3 · polymorphism

    private Explanation polymorphismZh(Finding f) {
        List<String> types = f.fact("types", Collections.<String>emptyList());
        int repeat = f.fact("repeatCount", 1);
        String kind = f.fact("kind", "instanceof");

        Explanation e = new Explanation();
        e.whatHappened = String.format("对 [%s] 的类型分派，%s。",
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
                "    class Circle implements Shape { public double area() { ... } }\n" +
                "    class Rect   implements Shape { public double area() { ... } }\n\n" +
                "然后直接调用 shape.area()，由 Java 在运行时选择正确的实现。\n" +
                "新增一种类型时，你只加一个类，一处都不用改。\n" +
                "\n" +
                "这就是多态存在的全部理由 —— 它是 C 那个 switch 分派的替代品，\n" +
                "不是什么高深概念。";
        e.caveat =
                "如果分支逻辑很简单、而且类型确定不会再增加，保留 " +
                (kind.equals("switch") ? "switch" : "if-else") + " 是可以的。\n" +
                "多态的代价是逻辑分散到多个文件，看全貌变难了。\n" +
                "判断标准：这组类型未来还会不会增加？";
        return e;
    }

    private Explanation polymorphismEn(Finding f) {
        List<String> types = f.fact("types", Collections.<String>emptyList());
        int repeat = f.fact("repeatCount", 1);
        String kind = f.fact("kind", "instanceof");

        Explanation e = new Explanation();
        e.whatHappened = String.format("Type dispatch over [%s], %s.",
                join(types, 5),
                repeat >= 2 ? "repeated in " + repeat + " places" : "via an " + kind + " chain");
        e.cInstinct =
                "In C this was your only option: put an int type field in the struct,\n" +
                "then switch(type) to dispatch to different handler functions.\n" +
                "It was the only way C could do \"same operation, different implementation\".\n" +
                "\n" +
                "Java gave you another way. You are still using the C one.";
        e.whyItMatters = repeat >= 2
                ? String.format(
                        "Every new type means editing all %d of these places.\n" +
                        "Miss one and the compiler stays silent — it only blows up at runtime\n" +
                        "when execution reaches that branch.\n" +
                        "That is exactly why Fowler lists it as a smell: the change is scattered.", repeat)
                : "Every new type adds another branch to this chain.\n" +
                  "The longer the chain, the easier it is to miss a case, and the compiler\n" +
                  "cannot help you.";
        e.suggestion =
                "Let each type implement the same method itself:\n\n" +
                "    interface Shape { double area(); }\n" +
                "    class Circle implements Shape { public double area() { ... } }\n" +
                "    class Rect   implements Shape { public double area() { ... } }\n\n" +
                "Then just call shape.area() and let Java pick the right implementation\n" +
                "at runtime. Adding a new type means adding one class and editing nothing.\n" +
                "\n" +
                "That is the entire reason polymorphism exists — it is the replacement for\n" +
                "that C switch dispatch, not some advanced concept.";
        e.caveat =
                "If the branches are trivial and the set of types genuinely will not grow,\n" +
                "keeping the " + (kind.equals("switch") ? "switch" : "if-else") + " is fine.\n" +
                "Polymorphism costs you something too: the logic spreads across files and\n" +
                "the whole picture becomes harder to see.\n" +
                "The question to ask: will this set of types keep growing?";
        return e;
    }

    // ================================================== Item 4 · static abuse

    private Explanation staticAbuseZh(Finding f) {
        String cls = f.fact("className", "该类");
        List<String> fields = f.fact("fields", Collections.<String>emptyList());

        Explanation e = new Explanation();
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
                "把 %s 的这些字段改成实例字段，然后把 %s 变成一个正常的对象。", cls, cls);
        e.caveat =
                "常量（static final）不在此列，那是正常且推荐的。\n" +
                "真正需要全局唯一的资源（连接池、配置），可以用单例，\n" +
                "但要把访问收敛到方法里，而不是直接暴露字段。";
        return e;
    }

    private Explanation staticAbuseEn(Finding f) {
        String cls = f.fact("className", "this class");
        List<String> fields = f.fact("fields", Collections.<String>emptyList());

        Explanation e = new Explanation();
        e.whatHappened = String.format(
                "%s has %d static, non-final field(s):\n%s",
                cls, fields.size(), "    " + String.join("\n    ", fields));
        e.cInstinct =
                "These are C global variables.\n" +
                "In C you wrote    int g_count;\n" +
                "In Java you wrote static int count;\n" +
                "Semantically the same thing: a block of memory any code can read or write.";
        e.whyItMatters =
                "Any line of code anywhere can change it, and when something goes wrong you\n" +
                "cannot tell who changed it or when. Debugging this class of bug means\n" +
                "reading the entire program.\n" +
                "If there are threads involved, it is a data race waiting to happen.";
        e.suggestion = String.format(
                "Ask yourself one question: who owns this state?\n\n" +
                "    Owned by some specific thing  ->  make it an instance field of that object\n" +
                "    Owned by the whole program    ->  it still needs an object to hold it,\n" +
                "                                      reachable only through methods\n\n" +
                "Turn %s's fields into instance fields, and turn %s into an ordinary object.",
                cls, cls);
        e.caveat =
                "Constants (static final) are not included here — those are normal and\n" +
                "encouraged.\n" +
                "For genuinely global resources (connection pools, configuration) a singleton\n" +
                "is acceptable, but keep access behind methods instead of exposing the field.";
        return e;
    }

    // ================================================== Item 5 · encapsulation

    private Explanation encapsulationZh(Finding f) {
        String cls = f.fact("className", "该类");
        List<String> fields = f.fact("fields", Collections.<String>emptyList());

        Explanation e = new Explanation();
        e.whatHappened = String.format("%s 有 %d 个 public 且可变的字段：\n%s",
                cls, fields.size(), fieldsBlock(fields));
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

    private Explanation encapsulationEn(Finding f) {
        String cls = f.fact("className", "this class");
        List<String> fields = f.fact("fields", Collections.<String>emptyList());

        Explanation e = new Explanation();
        e.whatHappened = String.format("%s has %d public mutable field(s):\n%s",
                cls, fields.size(), fieldsBlock(fields));
        e.cInstinct =
                "In C, struct members were public by definition — you wrote s.field = x\n" +
                "and the language offered nothing else.\n" +
                "You carried that habit straight into Java, simply swapping struct for class.";
        e.whyItMatters = String.format(
                "Any code anywhere can bypass %s and change its fields directly.\n" +
                "Which means: the day you want to add a rule (\"quantity must not be negative\")\n" +
                "there is nowhere to put it — no single door everything has to go through.", cls);
        e.suggestion = String.format(
                "Make those fields private, then compile.\n" +
                "Every compile error is a place where outside code was reaching into %s's\n" +
                "internal state.\n" +
                "\n" +
                "For each error, ask:\n" +
                "    \"Shouldn't %s be providing a method for this in the first place?\"\n" +
                "\n" +
                "Usually the answer is yes. This process pulls behaviour back to where the\n" +
                "data lives — which incidentally fixes check item 1 as well.", cls, cls);
        e.caveat =
                "public final fields are safe and not reported.\n" +
                "Pure data transfer objects (DTO/VO) need not be encapsulated; those are\n" +
                "already excluded by naming convention.\n" +
                "Also note: mechanically adding a getter and setter for every field is not\n" +
                "encapsulation. It is public spelled differently.";
        return e;
    }

    // ================================================== Item 6 · bloated main

    private Explanation mainBloatZh(Finding f) {
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

    private Explanation mainBloatEn(Finding f) {
        String cls = f.fact("className", "this class");
        int lines = f.fact("lines", 0);

        Explanation e = new Explanation();
        e.whatHappened = String.format("%s.main() is %d lines long.", cls, lines);
        e.cInstinct =
                "This is the shape of a C program: one main, executed top to bottom.\n" +
                "In C that was natural — a program was a stream of instructions.\n" +
                "You moved that stream verbatim into Java's main.";
        e.whyItMatters = String.format(
                "A %d-line method cannot be tested on its own, cannot be reused, and is hard\n" +
                "to read.\n" +
                "But the more important point: it shows this program has no notion of\n" +
                "\"objects\" yet — everything sits on a single timeline and nothing has an\n" +
                "identity of its own.", lines);
        e.suggestion =
                "Pick one continuous stretch of logic inside main and ask:\n\n" +
                "    \"What thing is this part dealing with?\"\n\n" +
                "That thing should be a class.\n" +
                "Move this logic into it, together with the few variables it keeps touching.\n" +
                "\n" +
                "Repeat a few times and main shrinks to a handful of lines: create the\n" +
                "objects, wire them together, start. That is all main should ever do.";
        e.caveat =
                "If this really is a throwaway script, a longer main is acceptable.\n" +
                "But past 100 lines there are almost always several distinct concepts\n" +
                "hiding inside.";
        return e;
    }

    // ================================================== helpers

    private String fieldsBlock(List<String> fields) {
        List<String> shown = fields.size() > 6 ? fields.subList(0, 6) : fields;
        String s = "    " + String.join("\n    ", shown);
        if (fields.size() > 6) {
            s += lang.pick("\n    … 另有 " + (fields.size() - 6) + " 个",
                           "\n    ... and " + (fields.size() - 6) + " more");
        }
        return s;
    }

    private String join(List<String> list, int max) {
        if (list.isEmpty()) return "";
        String sep = lang.pick("、", ", ");
        if (list.size() <= max) return String.join(sep, list);
        return String.join(sep, list.subList(0, max))
                + lang.pick(" 等 " + list.size() + " 项", " and " + (list.size() - max) + " more");
    }
}
