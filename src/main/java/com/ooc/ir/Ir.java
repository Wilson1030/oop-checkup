package com.ooc.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一中间表示（IR）。
 *
 * 规则引擎只认这里的模型，完全不知道底层用的是哪个解析器。
 * 将来若要更换解析器（JavaParser -> javac API -> 其他），
 * 只需重写 parse 层，本文件与所有规则一行都不用动。
 */
public final class Ir {

    private Ir() {}

    /** 方法参数 */
    public static final class Param {
        public final String type;
        public final String name;

        public Param(String type, String name) {
            this.type = type;
            this.name = name;
        }

        /** 用于参数团比对的键：类型和名称都必须一致 */
        public String key() {
            return type + " " + name;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    /** 字段 */
    public static final class Field {
        public final String name;
        public final String type;
        public final boolean isStatic;
        public final boolean isFinal;
        public final boolean isPublic;

        public Field(String name, String type, boolean isStatic, boolean isFinal, boolean isPublic) {
            this.name = name;
            this.type = type;
            this.isStatic = isStatic;
            this.isFinal = isFinal;
            this.isPublic = isPublic;
        }
    }

    /** 方法或构造器 */
    public static final class Method {
        public final String name;
        public final List<Param> params;
        public final String returnType;
        public final boolean isStatic;
        public final boolean isConstructor;
        public final boolean isAbstract;
        public final int bodyLines;
        public final int line;

        public Method(String name, List<Param> params, String returnType,
                      boolean isStatic, boolean isConstructor, boolean isAbstract,
                      int bodyLines, int line) {
            this.name = name;
            this.params = params;
            this.returnType = returnType;
            this.isStatic = isStatic;
            this.isConstructor = isConstructor;
            this.isAbstract = isAbstract;
            this.bodyLines = bodyLines;
            this.line = line;
        }

        public String signature() {
            return name + "(" + params.stream().map(Param::toString)
                    .collect(Collectors.joining(", ")) + ")";
        }
    }

    /** 类型声明（class / interface / enum / record） */
    public static final class Klass {
        public final String name;
        public final String qualifiedName;
        public final String filePath;
        public final int line;
        public final boolean isEnum;
        public final boolean isInterface;
        public final boolean isAbstract;
        public final boolean isRecord;
        /** @interface 注解声明 —— 不是类，没有行为是其语言定义 */
        public final boolean isAnnotation;
        public final boolean isPrivate;
        public final boolean isPublic;
        public final boolean isNested;
        public final boolean isStatic;
        public final List<String> annotations;
        public final List<Field> fields = new ArrayList<>();
        public final List<Method> methods = new ArrayList<>();

        public Klass(String name, String qualifiedName, String filePath, int line,
                     boolean isEnum, boolean isInterface, boolean isAbstract, boolean isRecord,
                     boolean isAnnotation, boolean isPrivate, boolean isPublic,
                     boolean isNested, boolean isStatic,
                     List<String> annotations) {
            this.name = name;
            this.qualifiedName = qualifiedName;
            this.filePath = filePath;
            this.line = line;
            this.isEnum = isEnum;
            this.isInterface = isInterface;
            this.isAbstract = isAbstract;
            this.isRecord = isRecord;
            this.isAnnotation = isAnnotation;
            this.isPrivate = isPrivate;
            this.isPublic = isPublic;
            this.isNested = isNested;
            this.isStatic = isStatic;
            this.annotations = annotations;
        }

        public List<Field> instanceFields() {
            return fields.stream().filter(f -> !f.isStatic).collect(Collectors.toList());
        }
    }

    /**
     * 一次带接收者的成员访问，形如 obj.foo() 或 obj.bar。
     *
     * 注意：阶段0没有类型解析（Symbol Solver），无法确定 obj 的真实类型。
     * 因此这是一个「启发式近似」：仅按成员名称匹配。
     * 同名字段分属不同类时会产生误差，报告中必须标注这一点。
     */
    public static final class Access {
        public final String fromClass;
        public final String memberName;
        public final boolean isCall;

        public Access(String fromClass, String memberName, boolean isCall) {
            this.fromClass = fromClass;
            this.memberName = memberName;
            this.isCall = isCall;
        }
    }

    /**
     * 一处基于类型的条件分派（instanceof 链 或 switch）。
     *
     * signature 是排序后的类型/标签集合，用于检测「同一组类型判断在多处重复」
     * —— 重复才是 Fowler 所说的真正痛点：新增一个类型要同时改多处。
     */
    public static final class TypeCheck {
        public enum Kind { INSTANCEOF_CHAIN, SWITCH }

        public final Kind kind;
        public final String signature;
        /** instanceof 链中出现的原始类型名（switch 为空），供规则层过滤 */
        public final List<String> rawTypes;
        public final int branches;
        public final int line;
        public final String method;
        public final String klass;
        public final String klassSimple;
        public final String file;

        public TypeCheck(Kind kind, String signature, List<String> rawTypes,
                         int branches, int line,
                         String method, String klass, String klassSimple, String file) {
            this.kind = kind;
            this.signature = signature;
            this.rawTypes = rawTypes;
            this.branches = branches;
            this.line = line;
            this.method = method;
            this.klass = klass;
            this.klassSimple = klassSimple;
            this.file = file;
        }
    }

    /** 整个被分析项目 */
    public static final class Project {
        public final List<Klass> classes = new ArrayList<>();
        public final List<Access> accesses = new ArrayList<>();
        public final List<TypeCheck> typeChecks = new ArrayList<>();
        public int effectiveLines;
        public int fileCount;
        public int parseFailures;
        public String rootPath = "";
    }
}
