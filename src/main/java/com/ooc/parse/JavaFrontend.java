package com.ooc.parse;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.ooc.ir.Ir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 解析层：JavaParser -> 统一 IR。
 *
 * 这是唯一与具体解析器耦合的文件。更换解析器只需重写本类。
 */
public final class JavaFrontend {

    private final JavaParser parser;
    private final boolean includeTests;

    public JavaFrontend(boolean includeTests) {
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(false);
        this.parser = new JavaParser(cfg);
        this.includeTests = includeTests;
    }

    public Ir.Project parse(Path root) throws IOException {
        Ir.Project project = new Ir.Project();
        project.rootPath = root.toAbsolutePath().toString();

        List<Path> files;
        try (Stream<Path> s = Files.walk(root)) {
            files = s.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(this::notExcludedPath)
                    .collect(Collectors.toList());
        }

        for (Path f : files) {
            String src;
            try {
                src = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            } catch (IOException e) {
                project.parseFailures++;
                continue;
            }
            project.fileCount++;
            project.effectiveLines += countEffectiveLines(src);

            ParseResult<CompilationUnit> result;
            try {
                result = parser.parse(src);
            } catch (Exception e) {
                project.parseFailures++;
                continue;
            }
            if (!result.isSuccessful() || result.getResult().isEmpty()) {
                project.parseFailures++;
                continue;
            }
            collect(result.getResult().get(), f.toString(), project);
        }
        return project;
    }

    private boolean notExcludedPath(Path p) {
        String s = p.toString().replace('\\', '/').toLowerCase();
        if (s.contains("/module-info.java") || s.endsWith("package-info.java")) return false;
        if (includeTests) return true;
        // 测试代码的写法惯例与业务代码不同，默认排除
        return !(s.contains("/test/") || s.contains("/tests/") || s.contains("/src/test/"));
    }

    /** 有效行 = 非空行，且不是纯注释行 */
    private int countEffectiveLines(String src) {
        int n = 0;
        for (String raw : src.split("\r?\n")) {
            String t = raw.trim();
            if (t.isEmpty()) continue;
            if (t.startsWith("//") || t.startsWith("/*") || t.startsWith("*") || t.startsWith("*/")) continue;
            n++;
        }
        return n;
    }

    @SuppressWarnings("rawtypes")
    private void collect(CompilationUnit cu, String file, Ir.Project project) {
        for (Object o : cu.findAll(TypeDeclaration.class)) {
            TypeDeclaration<?> td = (TypeDeclaration<?>) o;

            boolean isInterface = (td instanceof ClassOrInterfaceDeclaration)
                    && ((ClassOrInterfaceDeclaration) td).isInterface();
            boolean isEnum = td instanceof EnumDeclaration;
            boolean isRecord = td instanceof RecordDeclaration;
            boolean isAnnotation = td instanceof AnnotationDeclaration;
            boolean isAbstract = (td instanceof ClassOrInterfaceDeclaration)
                    && ((ClassOrInterfaceDeclaration) td).isAbstract();
            boolean isPrivate = td.getModifiers().stream()
                    .anyMatch(m -> m.getKeyword() == Modifier.Keyword.PRIVATE);
            boolean isPublic = td.getModifiers().stream()
                    .anyMatch(m -> m.getKeyword() == Modifier.Keyword.PUBLIC);
            boolean isStatic = td.getModifiers().stream()
                    .anyMatch(m -> m.getKeyword() == Modifier.Keyword.STATIC);
            boolean isNested = td.getParentNode().isPresent()
                    && td.getParentNode().get() instanceof TypeDeclaration;

            List<String> anns = td.getAnnotations().stream()
                    .map(ann -> ann.getNameAsString())
                    .collect(Collectors.toList());

            Ir.Klass k = new Ir.Klass(
                    td.getNameAsString(),
                    td.getFullyQualifiedName().orElse(td.getNameAsString()),
                    file,
                    line(td.getBegin()),
                    isEnum, isInterface, isAbstract, isRecord,
                    isAnnotation, isPrivate, isPublic, isNested, isStatic,
                    anns);

            // 只取直接成员，避免嵌套类被重复统计
            for (BodyDeclaration<?> member : td.getMembers()) {
                if (member instanceof FieldDeclaration) {
                    FieldDeclaration fd = (FieldDeclaration) member;
                    for (VariableDeclarator v : fd.getVariables()) {
                        k.fields.add(new Ir.Field(
                                v.getNameAsString(),
                                v.getTypeAsString(),
                                fd.isStatic(), fd.isFinal(), fd.isPublic()));
                    }
                } else if (member instanceof MethodDeclaration) {
                    MethodDeclaration md = (MethodDeclaration) member;
                    k.methods.add(new Ir.Method(
                            md.getNameAsString(),
                            params(md.getParameters()),
                            md.getTypeAsString(),
                            md.isStatic(), false, md.isAbstract(),
                            bodyLines(md.getBody()),
                            line(md.getBegin())));
                    md.getBody().ifPresent(b -> scanAccesses(b, k.qualifiedName, project));
                    md.getBody().ifPresent(b -> scanTypeChecks(
                            b, md.getNameAsString(), k, file, project));
                } else if (member instanceof ConstructorDeclaration) {
                    ConstructorDeclaration cd = (ConstructorDeclaration) member;
                    k.methods.add(new Ir.Method(
                            cd.getNameAsString(),
                            params(cd.getParameters()),
                            "void",
                            false, true, false,
                            bodyLines(Optional.of(cd.getBody())),
                            line(cd.getBegin())));
                    scanAccesses(cd.getBody(), k.qualifiedName, project);
                }
            }
            project.classes.add(k);
        }
    }

    private List<Ir.Param> params(com.github.javaparser.ast.NodeList<Parameter> ps) {
        List<Ir.Param> out = new ArrayList<>();
        for (Parameter p : ps) {
            out.add(new Ir.Param(p.getTypeAsString(), p.getNameAsString()));
        }
        return out;
    }

    /** 收集带显式接收者的成员访问（obj.foo() / obj.bar），排除 this. 与 super. */
    private void scanAccesses(BlockStmt body, String fromClass, Ir.Project project) {
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            if (hasExternalScope(call.getScope().orElse(null))) {
                project.accesses.add(new Ir.Access(fromClass, call.getNameAsString(), true));
            }
        }
        for (FieldAccessExpr fa : body.findAll(FieldAccessExpr.class)) {
            if (hasExternalScope(fa.getScope())) {
                project.accesses.add(new Ir.Access(fromClass, fa.getNameAsString(), false));
            }
        }
    }

    /**
     * 采集基于类型的条件分派。
     * 排除 equals() 内的 instanceof —— 那是 Java 规范要求的标准写法。
     */
    private void scanTypeChecks(BlockStmt body, String methodName, Ir.Klass k,
                                String file, Ir.Project project) {
        if (!methodName.equals("equals")) {
            List<IfStmt> all = body.findAll(IfStmt.class);
            Set<IfStmt> asElse = new HashSet<>();
            for (IfStmt s : all) {
                Statement e = s.getElseStmt().orElse(null);
                if (e instanceof IfStmt) asElse.add((IfStmt) e);
            }
            for (IfStmt head : all) {
                if (asElse.contains(head)) continue;              // 只处理链头
                List<String> types = new ArrayList<>();
                IfStmt cur = head;
                while (cur != null) {
                    for (InstanceOfExpr io : cur.getCondition().findAll(InstanceOfExpr.class)) {
                        types.add(io.getType().asString());
                    }
                    Statement els = cur.getElseStmt().orElse(null);
                    cur = (els instanceof IfStmt) ? (IfStmt) els : null;
                }
                if (types.size() >= 2) {
                    Set<String> sorted = new TreeSet<>(types);
                    project.typeChecks.add(new Ir.TypeCheck(
                            Ir.TypeCheck.Kind.INSTANCEOF_CHAIN,
                            String.join(",", sorted), new ArrayList<>(sorted), types.size(),
                            line(head.getBegin()), methodName,
                            k.qualifiedName, k.name, file));
                }
            }
        }

        for (SwitchStmt sw : body.findAll(SwitchStmt.class)) {
            Set<String> labels = new TreeSet<>();
            int branches = 0;
            for (com.github.javaparser.ast.stmt.SwitchEntry entry : sw.getEntries()) {
                if (entry.getLabels().isEmpty()) continue;        // default 不计
                branches++;
                entry.getLabels().forEach(l -> labels.add(l.toString()));
            }
            if (branches >= 2) {
                project.typeChecks.add(new Ir.TypeCheck(
                        Ir.TypeCheck.Kind.SWITCH,
                        String.join(",", labels), new ArrayList<>(labels), branches,
                        line(sw.getBegin()), methodName,
                        k.qualifiedName, k.name, file));
            }
        }
    }

    private boolean hasExternalScope(com.github.javaparser.ast.Node scope) {
        if (scope == null) return false;
        return !(scope instanceof ThisExpr) && !(scope instanceof SuperExpr);
    }

    private int bodyLines(Optional<BlockStmt> body) {
        if (body.isEmpty()) return 0;
        BlockStmt b = body.get();
        if (b.getBegin().isEmpty() || b.getEnd().isEmpty()) return 0;
        return Math.max(0, b.getEnd().get().line - b.getBegin().get().line - 1);
    }

    private int line(Optional<com.github.javaparser.Position> pos) {
        return pos.map(p -> p.line).orElse(0);
    }
}
