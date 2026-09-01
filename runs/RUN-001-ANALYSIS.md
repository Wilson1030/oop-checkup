# RUN-001 结果与失败分析

- 引擎版本：commit `afc3d4f`（阈值冻结于下载样本之前）
- 预注册：commit `13b2284`
- 运行时间：2026-09-01 15:03
- 原始输出：`runs/RUN-001-*.txt`

## 样本版本

| 样本 | 组 | commit |
|---|---|---|
| guava | A 对照 | `b3346e3` |
| junit5 | A 对照 | `483a6f0` |
| buaa-oo | B 学生 | `724782d` |
| lr580-hw | B 学生 | `bd60d12` |
| minigame | B 学生 | `1adb90f` |
| vinamdar-oop | B 学生 | `8cf3b7d` |

---

## 一、结果

| 样本 | 有效行 | 类 | R1 预测 | **R1 实际** | R2 预测 | **R2 实际** |
|---|---:|---:|---|---:|---|---:|
| guava | 6360 | 133 | ≤ 2 | **82**（红23） | ≤ 1 | **1**（红1） |
| junit5 | 7869 | 235 | ≤ 2 | **255**（红78） | ≤ 1 | **3**（红0） |
| buaa-oo | 1075 | 35 | 1–8 | **1** | 1–6 | **2** |
| lr580-hw | 2490 | 29 | 1–8 | **13** | 1–6 | **0** |
| minigame | 588 | 4 | 0–3 | **1** | 0–3 | **2** |
| vinamdar-oop | 139 | 3 | 0–5 | **0** | 1–6 | **0** |

### 判定

| 条件 | 状态 | 证据 |
|---|---|---|
| **F1**　A 组 R1 ≥ 5 组 | 🔴 **触发** | guava 82、junit5 255 |
| **F2**　A 组均值 ≥ B 组均值 | 🔴 **触发** | R1：168.5 vs 3.75（反向且相差 45 倍）<br>R2：2.0 vs 1.0（反向） |
| F3　B 组全零 | 未触发 | — |
| F4　B 组无差异 | 未触发 | R1 分布 0/1/1/13，有差异 |

**按预注册判定：阶段 0 失败。** F1、F2 均为硬性终止条件，不允许通过调整阈值挽救。

---

## 二、失败原因（有明确证据，非事后猜测）

### R1 参数团 —— 规则定义存在结构性错误

**证据 A：Guava，同一组合出现 69 次**

```
Preconditions.java:159  checkArgument(boolean expression, String errorMessageTemplate, Object errorMessageArgs)
Preconditions.java:176  checkArgument(boolean expression, String errorMessageTemplate, char p1)
Preconditions.java:190  checkArgument(boolean expression, String errorMessageTemplate, int p1)
...（共 69 处，全部是 checkArgument）
```

这是 Guava 为避免 varargs 装箱开销而手写的**方法重载家族**，属于教科书级的优秀设计。

**证据 B：JUnit5，同一组合出现 28 次**

```
AssertArrayEquals.java:184  assertArrayEquals(boolean[] expected, boolean[] actual, Deque<Integer> indexes, Object messageOrSupplier)
AssertArrayEquals.java:206  assertArrayEquals(char[]    expected, char[]    actual, Deque<Integer> indexes, Object messageOrSupplier)
...（重载家族 + 同类内私有辅助方法链）
```

`(indexes, messageOrSupplier)` 是递归下降比较时携带的上下文，全部位于同一个类内部。

**证据 C：对照——学生代码的真实参数团**

```
Init.java:109   update_db_settings(String ip, String port, String db, String user, String psw, String cfg)
Link.java:33    create_database  (String ip, String port, String db, String name, String psw)
Link.java:41    connect          (String ip, String port, String db, String name, String psw, String cfg)
SqlIO.java:4    get_export_head  (String ip, String port, String name, String psw)
SqlIO.java:8    exportAll        (String ip, String port, String db, String name, String psw, String path)
SqlIO.java:17   export           (String ip, String port, String db, String name, String psw, String path, String[] dt)
SqlIO.java:30   importAll        (String ip, String port, String db, String name, String psw, String path)
```

**三个不同的类、七个不同名的方法**共享同一组数据库连接参数 —— 这是真正的参数团。

### 关键区别

| | 开源库（应排除） | 学生代码（应检出） |
|---|---|---|
| 方法名 | 全部相同（重载） | 各不相同 |
| 所在类 | 同一个类 | 跨越 3 个类 |
| 本质 | API 设计的一致性 | 数据被拆散后到处传 |

**当前规则完全没有这两个约束，因此把"设计一致性"误判为"坏味道"。**

### R2 贫血模型 —— 两个实现缺陷

| 样本 | 检出 | 判定 |
|---|---|---|
| junit5 `TempDir` / `RepeatedTest` / `Timeout` | 3 项 | ❌ **误报**：这三个都是 `@interface` 注解声明，被当成了普通类 |
| guava `ValueHolder` | 1 项 | ❌ **误报**：`MoreObjects` 的私有静态嵌套节点类，贫血是刻意设计 |
| buaa-oo `Global` ×2 | 2 项 | ✅ 合理：全局变量容器，确为坏味道（两份来自不同作业目录的同名类） |
| minigame `Board` / `Board_Remember` | 2 项 | ⚠️ 待人工核对 |

R2 的判据本身没错，是实现漏了两类排除。

---

## 三、失败性质的区分（这决定下一步能做什么）

| 规则 | 失败性质 | 允许的修复方式 |
|---|---|---|
| **R1** | **判据定义错误** —— 方向反了，调阈值救不了（阈值提到 100，学生代码就全部归零） | 必须**重写判据**，并**重新预注册**后再跑 |
| **R2** | **实现缺陷** —— `@interface` 不是类，这是客观错误 | 可直接修复，**不算调参** |

**必须区分"修 bug"和"调参"。** 排除 `@interface` 是修正客观错误；而如果把 R1 的阈值从 3 提到 100 让 Guava 好看，那就是本预注册要防止的行为。

---

## 四、拟议的 R1 新判据（尚未实现，须先重新预注册）

一个参数组合只有**同时**满足以下条件才计为参数团：

1. **跨类**：出现在 ≥ 2 个不同的类中
2. **非重载**：涉及 ≥ 2 个不同的方法名
3. **密度归一化**：以「每千行有效代码的检出数」计，取代绝对计数

按新判据回溯验证：

| 案例 | 跨类 | 非重载 | 结果 |
|---|:---:|:---:|---|
| Guava `checkArgument` | ❌ 同一类 | ❌ 同一方法名 | 排除 ✅ |
| JUnit5 `assertArrayEquals` | ❌ 同一类 | ❌ 同一方法名 | 排除 ✅ |
| 学生 `(ip, port)` | ✅ 3 个类 | ✅ 7 个方法名 | 保留 ✅ |

## 五、拟议的 R2 修复

1. 排除 `@interface`（`AnnotationDeclaration`）
2. 排除私有静态嵌套类（`private static class`）—— 这类内部数据节点贫血是正当的

---

## 六、结论

阶段 0 首轮判定**失败**，但失败本身给出了三条此前无法得知的信息：

1. **绝对计数指标在教学工具中不可用** —— 学生项目天然比开源库小一个数量级
2. **方法重载与参数团在语法层面完全同形**，必须用「跨类 + 非重载」区分
3. **对照组的价值已被证实** —— 若只跑学生代码，R1 的 13 项检出看起来完全合理，这个致命缺陷不会被发现

下一轮（RUN-002）必须先更新预注册、重新写下预测，再实现修改。
