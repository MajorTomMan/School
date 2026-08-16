# School 课程包契约

## 总原则

APK 是唯一课程运行引擎，也是课程包能否启用的最终权威。课程包只描述教学内容和已注册基础设施的语义参数，不携带 Kotlin、JavaScript、Python 或其他可执行代码，也不能借可视化调用访问 App 内部能力。

当前 `course.json` 根节点必须且只能包含：

```json
{
  "textbook": {},
  "knowledgePoints": [],
  "chapters": []
}
```

未知字段不会被忽略。解析、关系校验、可视化 schema 校验和教材文件校验任一失败，整个暂存课程都不能启用。

## 教材

`textbook` 必须且只能包含：

```json
{
  "id": "pep-math-7-1",
  "title": "义务教育教科书·数学七年级上册",
  "publisher": "人民教育出版社",
  "edition": "2024",
  "grade": "七年级",
  "semester": "上册",
  "subject": "数学",
  "pdf": {
    "path": "assets/textbook.pdf",
    "pageCount": 202,
    "pageIndexOffset": 7
  }
}
```

`pdf.path` 必须是课程包内部相对 PDF 路径，不能是绝对路径，不能包含 `..`。URL、大小和 SHA-256 属于包外分发清单，不进入课程业务数据。

`references.pageStart/pageEnd` 使用教材印刷页语义；`pageIndexOffset` 负责把教材印刷页映射到 PDF 实际页。例如七上数学中印刷第 1 页对应 PDF 第 8 页。

## 知识点

每个知识点必须且只能包含：

```json
{
  "id": "positive-negative",
  "name": "正数和负数",
  "description": "表示相反意义的量",
  "prerequisiteIds": []
}
```

要求：

- ID 唯一；
- 前置 ID 必须存在；
- 前置关系不能成环。

## 章节、课时

结构固定为：

```text
chapters[]
└── sections[]
    └── lessons[]
```

章节字段：

```json
{
  "id": "chapter-01",
  "title": "第一章 有理数",
  "sections": []
}
```

小节字段：

```json
{
  "id": "section-01-01",
  "title": "1.1 正数和负数",
  "lessons": []
}
```

课时字段必须且只能是：

```json
{
  "id": "positive-negative-intro",
  "title": "正数和负数",
  "aliases": [],
  "goals": [],
  "knowledgePointIds": [],
  "prerequisiteLessonIds": [],
  "references": [],
  "steps": [],
  "practice": [],
  "summary": []
}
```

课时 ID 必须唯一；知识点绑定和前置课时必须存在；教学目标、教学步骤和总结不能为空。

## 教材引用

课时可使用：

```json
{
  "label": "教材第2—5页",
  "pageStart": 2,
  "pageEnd": 5
}
```

要求 `pageStart <= pageEnd <= textbook.pdf.pageCount`。具体课程还应按该教材的印刷页范围做内容审校，不能把 PDF 索引页直接写进教材引用。

## 教学步骤

允许的步骤类型只有：

- `explanation`
- `question`
- `keyIdea`
- `formula`
- `example`
- `visualization`
- `checkpoint`
- `summary`

旧的 `scene/template/data` 已移除，**不提供兼容解析**。

### explanation

```json
{
  "type": "explanation",
  "title": "可选标题",
  "text": "讲解正文"
}
```

### question

```json
{
  "type": "question",
  "prompt": "低于0℃怎么表示？",
  "hint": "可选提示"
}
```

### keyIdea

```json
{
  "type": "keyIdea",
  "title": "核心概念",
  "text": "……"
}
```

### formula

```json
{
  "type": "formula",
  "expression": "a+b",
  "note": "可选说明"
}
```

`expression` 保存纯 LaTeX 数学内容，不添加 `$...$`、`\(...\)` 等分隔符；中文解释放在 `note` 或其他正文步骤中。

### example

```json
{
  "type": "example",
  "title": "例1",
  "prompt": "……",
  "steps": ["第一步", "第二步"],
  "answer": "……"
}
```

### visualization

可视化是独立 `:visualization` 基础设施。课程只能传入：

```json
{
  "type": "visualization",
  "renderer": "mathematics.number-line.opposite",
  "parameters": {
    "value": 3,
    "min": -8,
    "max": 8,
    "step": 1
  },
  "texts": {
    "title": "相反数关于 0 对称",
    "leftLabel": "−3",
    "rightLabel": "3",
    "note": "两个点到 0 的距离相等。"
  }
}
```

这四个字段必须完整且不能增加第五个字段。每个 renderer 都有独立、严格的字段和类型 schema；课程不能自行创造参数。

当前公开的参数值类型只有：

- JSON number；
- JSON boolean；
- 只含 number 的 JSON array；
- **仅在 schema 明确声明为 `MATH_EXPRESSION` 的字段中**使用受限数学表达式字符串。

例如函数图：

```json
{
  "type": "visualization",
  "renderer": "mathematics.function.graph",
  "parameters": {
    "expression": "sin(x)+x^2/4",
    "xMin": -6,
    "xMax": 6,
    "yMin": -4,
    "yMax": 10
  },
  "texts": {
    "title": "函数图像"
  }
}
```

这里的 `expression` **不是字符串脚本**。它由可视化模块的安全数学 Parser/AST 解析，只允许数字、受限变量、`+ - * / ^`、括号以及固定数学函数白名单；不允许语句、属性访问、索引、集合、lambda、任意函数调用、反射或宿主 API。

除 schema 明确声明的数学表达式字段外，普通 parameter 不接受任意字符串。

所有 visualization invocation 均不接受：

- object / 任意 map 作为参数值；
- null；
- URL；
- 文件路径；
- Android 资源 ID；
- 类名；
- 回调；
- JavaScript / Kotlin Script / Python 或其他代码；
- 混合类型数组。

`texts` 的 value 只能是字符串，而且只作为显示文字使用，不解释成路径、命令、表达式或数据源。

未知 renderer、未知参数、未知文本、缺少 required 字段、类型错误或语义越界都会使课程包校验失败。完整 renderer 规则见 `docs/VISUALIZATION_FRAMEWORK.md`。

### checkpoint

```json
{
  "type": "checkpoint",
  "prompt": "……",
  "expectedAnswer": "……",
  "explanation": "……"
}
```

### summary

```json
{
  "type": "summary",
  "text": "……"
}
```

## 练习

课时内练习必须且只能包含：

```json
{
  "id": "practice-01",
  "prompt": "向西8米怎么表示？",
  "answer": "-8米",
  "analysis": ["先确定题目规定的正方向", "向西与正方向相反，因此用负数表示"],
  "knowledgePointIds": ["positive-negative"],
  "difficulty": 1
}
```

要求：

- `difficulty` 为 1..5；
- 知识点 ID 必须存在；
- `analysis` 不能为空；
- 练习 ID 稳定且唯一；
- `answer` 与 `analysis` 分离，解析不能夹带编辑过程或临时核对文字。

## 可视化与课程业务隔离

课程包不拥有绘图实现，也不能指定像素、Composable、Canvas 函数或任意 DSL。

课程层只表达：

```text
renderer = 用哪一种已注册基础设施能力
parameters = schema 允许的数学 / 几何 / 教学语义参数
texts = 教学显示文字
```

可视化层只消费这个不可执行调用对象。它不能访问：

- 网络；
- 文件系统；
- ContentResolver；
- 数据库 / DataStore；
- AI；
- Repository / ViewModel；
- 课程下载器；
- 外部动态数据。

因此课程内容和渲染基础设施之间保持单向、可验证、可测试的边界。

## Authored source 与发布产物

仓库中的正式课程使用可读、可 diff、可审查的 `courses/<textbook-id>/course.json` 作为 authored source。

压缩包、gzip/base64 分卷、文件 SHA-256、不可变 release ID 和外部分发 URL 都属于发布阶段产物，不作为课程源码维护。旧 `course.json.gz.b64.part*` authored source 已退役，不提供兼容。

## APK 启用前校验

课程进入 `active` 前至少完成：

1. 分发清单文件集合、大小和 SHA-256 校验；
2. `course.json` 根字段精确匹配；
3. 教材字段和 PDF 相对路径校验；
4. 知识点 ID、前置关系和循环依赖校验；
5. 章节、小节、课时关系校验；
6. 教材引用页码校验；
7. 教学步骤类型校验；
8. `visualization` renderer、字段类型和语义 schema 校验；
9. 练习知识点与难度校验；
10. 教材 PDF 实际存在；
11. 全部通过后才原子切换到 `active`。

任何旧 `scene`、未知 renderer、越界可视化参数或可执行内容都不能通过安装校验。
