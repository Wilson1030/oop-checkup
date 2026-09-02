package com.oopc.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unified intermediate representation (IR).
 *
 * The rule engine only knows this model; it has no idea which parser produced it.
 * To later swap parsers (JavaParser -> javac API -> other), only the parse layer
 * needs rewriting; this file and every rule stay untouched.
 */
public final class Ir {

    private Ir() {}

    /** A method parameter. */
    public static final class Param {
        public final String type;
        public final String name;

        public Param(String type, String name) {
            this.type = type;
            this.name = name;
        }

        /** Key used for data-clump matching: both type and name must match. */
        public String key() {
            return type + " " + name;
        }

        @Override
        public String toString() {
            return type + " " + name;
        }
    }

    /** A field. */
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

    /** A method or constructor. */
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

    /** A type declaration (class / interface / enum / record). */
    public static final class Klass {
        public final String name;
        public final String qualifiedName;
        public final String filePath;
        public final int line;
        public final boolean isEnum;
        public final boolean isInterface;
        public final boolean isAbstract;
        public final boolean isRecord;
        /** @interface annotation declaration - not a class; having no behaviour is its language definition */
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
     * A member access with a receiver, e.g. obj.foo() or obj.bar.
     *
     * Note: stage 0 has no type resolution (Symbol Solver), so obj's real type
     * cannot be determined. This is therefore a heuristic approximation: it
     * matches on member name only. Identically named fields in different classes
     * cause error; the report must call that out.
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
     * A type-based conditional dispatch (instanceof chain or switch).
     *
     * signature is the sorted set of types/labels, used to detect "the same type
     * dispatch repeated in several places" - repetition is the real pain point
     * Fowler names: adding a type requires editing several places at once.
     */
    public static final class TypeCheck {
        public enum Kind { INSTANCEOF_CHAIN, SWITCH }

        public final Kind kind;
        public final String signature;
        /** Raw type names in an instanceof chain (empty for switch), for the rule layer to filter. */
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

    /** The whole analysed project. */
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
