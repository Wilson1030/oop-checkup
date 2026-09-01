# 上手手册

从零跑通整个流程。每一步都给了**预期输出**，对不上就说明出问题了。

> 全程复制粘贴即可。遇到问题看最后的「排错」。

---

## 第 0 步 · 检查环境

```powershell
java -version
mvn -v
git --version
```

**预期**：

```
java version "17" 或更高
Apache Maven 3.x
git version 2.x
```

- `java` 没有 → 装 JDK 17+
- `mvn` 提示找不到命令 → 见排错 ①
- 只是想**用**不想改代码 → 只要 `java` 就够，跳到第 2 步

---

## 第 1 步 · 构建

```powershell
cd C:\Users\gx293\Desktop\oo-checkup
mvn package
```

第一次会下载 JavaParser 依赖，约 1-2 分钟。

**预期**：最后出现 `BUILD SUCCESS`，并生成 `target\oo-checkup.jar`（约 1.5M）。

看到 `BUILD FAILURE` → 见排错 ②

---

## 第 2 步 · 跑演示：先看「坏代码」

**用启动脚本，编码问题已经帮你处理好了：**

```powershell
.\checkup.bat examples\before
```

> macOS / Linux 用 `./checkup.sh examples/before`
> PowerShell 里 `.\` 前缀不能省；CMD 里可以直接写 `checkup examples\before`

**预期**（检查表部分）：

```
  1. 数据与行为是否结合 .......... 2 处违反
  2. 是否避免散装参数传递 ........ 1 处待确认
  3. 是否用多态替代类型判断 ...... 1 处违反
  4. static 是否被滥用 ........... 2 处违反
  5. 封装是否完整 ................ 1 处违反
  6. 入口方法是否过度承担 ........ 1 处违反
  7. 是否避免基本类型偏执 ........ 未实现

  违反 5 项，待确认 1 项
```

中文乱码 → 见排错 ③

### 看全部细节

默认每项只展开 3 处：

```powershell
.\checkup.bat examples\before --detail 20
```

### 建议：导出到文件慢慢看

```powershell
.\checkup.bat examples\before --detail 20 > before.txt
```

写文件时编码不受终端影响，**一定不会乱码**。用 VS Code 打开看。

---

## 第 3 步 · 再看「改好之后」

```powershell
.\checkup.bat examples\after
```

**预期**：

```
  1. 数据与行为是否结合 .......... 通过
  2. 是否避免散装参数传递 ........ 1 处待确认
  3. 是否用多态替代类型判断 ...... 通过
  4. static 是否被滥用 ........... 通过
  5. 封装是否完整 ................ 通过
  6. 入口方法是否过度承担 ........ 通过

  违反 0 项，待确认 1 项
```

同一个图书管理系统，功能完全一样，只是重构了。

**这一步比第 2 步更重要**——它证明工具不会对好代码乱叫。

### 想搞清楚差在哪，就对着看这两组文件

| 检查项 | before | after |
|---|---|---|
| 1 数据与行为 | `Book.java` 只有 getter/setter | `Book.java` 有 `lend()` / `giveBack()` / `isAvailable()` |
| 3 多态 | `SearchService.java` 两处 `switch` | `SearchStrategy` 接口 + 3 个实现类 |
| 4 static | `LibraryService` 全 static + 全局变量 | `Library` 是普通对象 |
| 5 封装 | `Book.title` 是 `public` | 全部 `private final` |
| 6 main | `Main.main()` 85 行 | `Main.main()` 12 行 |

### 两份都能编译，可以自己验证不是摆设

```powershell
javac -encoding UTF-8 -d out\before examples\before\library\*.java
javac -encoding UTF-8 -d out\after  examples\after\library\*.java
```

---

## 第 4 步 · 自己验证「同一份代码跑两次结果是否一样」

不要信我说的，自己验：

```powershell
.\checkup.bat examples\before --detail 20 > r1.txt
.\checkup.bat examples\before --detail 20 > r2.txt
fc r1.txt r2.txt
```

**预期**：`FC: 找不到差异` / `no differences encountered`

Git Bash 里用 `diff r1.txt r2.txt`，无输出即一致。

换个大项目再验一次（需要先有 `samples\junit5`，见第 6 步）。

### 为什么能保证

| | |
|---|---|
| 随机数 | 全程未使用 |
| LLM | 未接入（接入后**只有解释文字**会变，检出条目和行号不变） |
| 时间戳 | 报告中不打印 |
| 哈希表顺序 | 一律用 `LinkedHashMap` / `TreeSet` |

---

## 第 5 步 · 跑你自己的代码

```powershell
.\checkup.bat "C:\你的作业目录" --detail 20 > 报告.txt
```

**路径有中文或空格时一定要加引号。**

路径给到**项目根目录**就行，会自动递归找所有 `.java`。

| 参数 | 什么时候用 |
|---|---|
| `--detail 20` | 想看全部检出 |
| `--include-tests` | 想连测试代码一起查（默认跳过 `src/test/`） |
| `--summary` | 只要一行结果 |

### 报告怎么读

先看检查表，**只挑「严重」的第一条动手**。

每条发现有五段：

```
▸ 发生了什么        客观事实 + 行号，先确认它没说错
▸ 你在 C 里会怎么写   ← 这段是关键，先读它
▸ 为什么是问题       具体后果
▸ 试试              照着做一步
▸ 但要注意           ← 别跳过，防止改过头
```

**「但要注意」那段一定要读完。**比如检查项 1 会建议你把方法搬进实体类，
但涉及数据库、网络、界面的逻辑留在 Service 里才是对的——这段就是说这个的。

---

## 第 6 步 · 复现验证过程（可选）

想确认「97.8% 准确率」不是我编的：

```powershell
# 下载一个第三方开源库（对照组，公认写得好）
mkdir samples
cd samples
git clone --depth 1 --filter=blob:none --sparse https://github.com/google/guava.git guava
cd guava
git sparse-checkout set guava/src/com/google/common/base
cd ..\..

# 跑它 —— 好代码应该几乎没有检出
.\checkup.bat samples\guava --summary
```

**预期**：

```
guava   行 6360 类 133 |1:0(0) |2:61? |3:1(0) |4:1(0) |5:0(0) |6:0(0)
```

读法：`|检查项:违反数(严重数)`，`?` 结尾的是待确认。

**要看的重点**：检查项 1、5、6 全是 0。Guava 是公认写得好的库，
**工具在它身上应该基本闭嘴**——这是这个工具的生死线。

检查项 2 那个 61 是待确认项，不计违反（它是语义类判据，做不准，所以不断言）。

### 完整验证记录

```
runs/RUN-001-ANALYSIS.md  ~  RUN-006-ANALYSIS.md   六轮结果与逐条核验
PREREGISTRATION.md ~ -v6.md                        每轮改代码之前写死的预测
```

按顺序读能看到完整过程，包括失败五次和一次「发现能调参让数字好看但拒绝了」的记录。

---

## 排错

### ① `mvn` 提示找不到命令

Maven 的环境变量只对**新开的终端**生效。关掉当前窗口重开一个。

还不行就用全路径：

```powershell
C:\Users\gx293\tools\apache-maven-3.9.16\bin\mvn package
```

### ② `BUILD FAILURE`

看错误信息里有没有 `Could not resolve dependencies` —— 那是下载 JavaParser 失败。

国内网络慢的话，新建 `C:\Users\gx293\.m2\settings.xml`：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### ③ 中文乱码

**用 `checkup.bat` / `checkup.sh` 就不会遇到**，它们已经把编码处理包进去了。

如果你坚持直接调 `java`：

```powershell
chcp 65001                                 # 切编码，一个窗口执行一次即可
java -jar target\oo-checkup.jar <路径>
```

或者直接导出到文件（**最省事，不受终端影响**）：

```powershell
java -jar target\oo-checkup.jar <路径> > 报告.txt
```

### ④ PowerShell 报 `找不到或无法加载主类 .encoding=UTF-8`

完整报错长这样：

```
错误: 找不到或无法加载主类 .encoding=UTF-8
原因: java.lang.ClassNotFoundException: /encoding=UTF-8
```

**原因**：PowerShell 会把 `-Dfile.encoding=UTF-8` 从 `.` 处**拆成两个参数**：

```
不加引号  →  [-Dfile]  [.encoding=UTF-8]  [-jar]  ...
                        ↑ java 把它当成了主类名
加引号    →  [-Dfile.encoding=UTF-8]  [-jar]  ...   ✓
```

CMD 不会这样，只有 PowerShell 会。

**三种解法**：

```powershell
.\checkup.bat <路径>                                    # 1. 用脚本（推荐）
java "-Dfile.encoding=UTF-8" -jar target\oo-checkup.jar <路径>   # 2. 加引号
java -jar target\oo-checkup.jar <路径> > 报告.txt        # 3. 导出文件，压根不需要这参数
```

### ⑤ 报错说路径非法 `Illegal char <"> at index 0`

引号被当成路径的一部分了。检查是不是嵌套了引号，或者在 Git Bash 里对引号做了 `\"` 转义。

PowerShell 里正确写法：

```powershell
.\checkup.bat "C:\Users\gx293\Desktop\我的作业"
```

### ⑥ 报告说「项目规模过小」

不足 80 有效行时不下结论——50 行的程序全塞在 `main` 里本来就是合理的，
报出来只会误导。

### ⑦ 检出为空

- 确认路径下真的有 `.java` 文件
- 报告开头会写「N 个文件 · N 有效行」，如果是 0 说明路径不对
- 测试代码默认跳过，加 `--include-tests`

### ⑧ 觉得某条报错了

**很有可能你是对的。**已知局限：

- 检查项 1 的「外部访问次数」是按成员名匹配的近似值，不同类的同名字段会串
- 检查项 2、7 是语义类判据，所以只输出「待确认」不断言违反
- 检查项 2 要求参数团**跨 ≥2 个类**才报。如果你的代码是「一个 Manager 类包打天下」，
  同一组参数即使传了七八个方法也不会报——这是已知的漏报场景
- 已知残留误报 1 条（Guava `Suppliers` 的内部优化路径）

发 issue 时请附上：**触发的代码片段 + 你认为它不该报的理由**。
判据的正当性必须能脱离具体样本论证——这是这个项目的规矩。

---

## 一页速查

```powershell
mvn package                                    # 构建

.\checkup.bat <路径>                           # 跑
.\checkup.bat <路径> --detail 20               # 看全部
.\checkup.bat <路径> --detail 20 > 报告.txt     # 导出（推荐）
.\checkup.bat <路径> --summary                 # 一行摘要
.\checkup.bat <上级目录> --batch --summary      # 批量对比
```

macOS / Linux 把 `.\checkup.bat` 换成 `./checkup.sh`，路径分隔符换成 `/`。

| 状态 | 含义 |
|---|---|
| `通过` | 没有检出 |
| `N 处违反` | 违反了对应标准，可追溯到文献出处 |
| `N 处待确认` | 语义类判据，工具不断言，交由你判断 |
| `未实现` | 该检查项尚未开发 |
