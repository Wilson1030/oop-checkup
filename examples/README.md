# 演示代码

`before/` 与 `after/` 是同一个图书管理系统的两个版本，用于展示工具的输出效果。

| | 说明 |
|---|---|
| `before/` | 刻意写成「用 Java 写 C」的形态：全局 static、贫血实体类、public 裸字段、switch 类型分派、逻辑全在 main |
| `after/` | 同样的功能，按面向对象重构后的版本 |

两份都能编译运行：

```bash
javac -encoding UTF-8 -d out before/library/*.java
javac -encoding UTF-8 -d out after/library/*.java
```

跑一下对比：

```bash
java -jar ../target/oo-checkup.jar before
java -jar ../target/oo-checkup.jar after
```

## ⚠️ 这些不是验证样本

本目录的代码是**为了展示效果而写的演示代码**。

工具的准确率验证**只使用第三方代码**（2 个成熟开源库 + 11 个真实学生仓库），
详见 `../runs/`。用自己写的样本去验证自己写的工具是循环论证，那个界限不能破。
