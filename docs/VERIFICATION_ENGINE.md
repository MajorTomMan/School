# Verification Engine

## 定位

Verification Engine 是 App 内各学科本地验证能力的公共协议层。它负责统一输入、结果、步骤、警告和可视化请求的结构，不负责把不同学科强行塞进同一个求解器。

当前开发顺序：

1. 建立 Verification Core。
2. 数学作为第一套完整 `SubjectEngine`。
3. 后续把现有物理、化学、生物确定性内核适配进同一协议。
4. 英语、日语在语言框架确定后接入。
5. 语文最后设计，并允许使用确定型、语境型、证据型三种验证模式。

## 公共协议

入口：

```text
VerificationRequest
        ↓
SubjectEngine
        ↓
VerificationResult
```

`VerificationResult` 统一包含：

- `subject`：学科。
- `mode`：验证真值模式。
- `status`：成功、暂不支持或输入无效。
- `problemType`：识别出的题型。
- `normalizedInput`：标准化输入。
- `answer`：强类型结果对象。
- `steps[]`：结构化推理步骤。
- `warnings[]`：条件、边界和能力提醒。
- `visualizations[]`：语义化可视化请求。

### VerificationStep

步骤不是 `List<String>`，而是结构化对象：

```text
VerificationStep
├── rule
├── title
├── before
├── after
├── explanation
├── conditions[]
└── children[]
```

`rule` 使用稳定的规则 key，例如：

- `STANDARDIZE_EQUATION`
- `TRANSPOSE_TERMS`
- `DIVIDE_BOTH_SIDES`
- `CALCULATE_DISCRIMINANT`
- `APPLY_QUADRATIC_FORMULA`
- `EXPAND_AND_COMBINE`

这样 UI 可以展示步骤，测试可以验证规则链，未来也可以对单个步骤继续挂载解释和可视化，而不需要解析自然语言字符串。

### VerificationArtifact

公共层禁止使用 `Any`、任意 Map 或脚本对象表示学科数据。Artifact 采用 sealed 类型，例如：

- `MathExpression`
- `MathEquation`
- `MathFunction`
- `MathSolution`
- `PhysicalRelation`
- `ChemicalEquation`
- `BiologyRelation`

各学科可以继续扩展自己的领域模型，但跨层输出必须保持明确的类型边界。

## 验证模式

公共协议预留三种模式：

- `DETERMINISTIC`：有明确规则和可复核结果。当前数学、未来多数理化生验证属于这一类。
- `CONTEXTUAL`：结果依赖语境、语体或使用条件，主要为未来语言学科预留。
- `EVIDENCE_BASED`：评价结论是否有充分证据支持，主要为未来开放文本分析预留。

当前数学只使用 `DETERMINISTIC`。

## 数学 Engine

`MathVerificationEngine` 的目标是本地覆盖初中和高中数学，不向完整 CAS 或 Wolfram Alpha 范围扩张。

### 当前接入能力

- 数值表达式。
- 精确有理数运算。
- 基础根式和常量表达式。
- 单变量多项式展开、合并同类项。
- 一元一次方程。
- 一元二次方程。
- `y=f(x)` / `f(x)=...` 和可识别的 `x` 函数。
- 函数图像。

数学 Engine 优先复用现有科学内核：

- `BigRational`
- `ScienceExpression`
- `ScienceExpressionParser`
- `ScienceExpressionSimplifier`
- `Polynomial`
- `AlgebraSolver`

不再在验证页面内部维护另一套数学 parser 或 equation solver。

### 明确范围边界

当前本地数学验证限定为初高中知识，不支持：

- 极限
- 导数 / 微分
- 积分
- 微分方程
- 无穷级数
- 其他高等数学内容

超出范围必须返回 `UNSUPPORTED`，不能偷偷调用 AI 或网络猜答案。

## 可视化边界

Verification Engine 不绘图，也不知道 Canvas、像素、采样实现或布局参数。

它只能返回：

```text
VerificationVisualizationRequest
├── renderer
├── parameters
└── texts
```

例如函数：

```text
renderer = mathematics.function.graph
expression = x^2 - 4
```

UI 再把这个语义请求转换为 visualization 模块的 `VisualizationInvocation`。实际绘制仍由独立 visualization 基础设施完成。

这保持既有约束：

> 学科引擎只描述数学/教学语义；绘制实现、网络、存储、AI 和代码执行能力留在边界之外。

## 后续理化生

物理、化学、生物仓库中已经存在确定性领域内核。后续不重写它们，而是增加适配层：

```text
Physics Core   ─┐
Chemistry Core ─┼→ SubjectEngine → VerificationResult
Biology Core   ─┘
```

物理继续拥有物理量、单位、量纲和物理规律；化学继续拥有化学式、元素计数、方程式和化学计量；生物继续拥有自身结构和规则推理。公共层只统一结果和步骤协议。

## 禁止事项

Verification Core 和本地学科 Engine 不得：

- 调用 AI 作为隐式 fallback。
- 访问网络来补全确定性结果。
- 执行 JavaScript、Kotlin Script、Python 或其他任意代码。
- 通过反射加载课程包指定的类。
- 把 Canvas / UI 实现细节暴露给学科输入。
- 用一个万能 `UniversalSolver` 代替独立学科规则。
