# School 课程包契约

## 总原则

APK 是唯一课程运行引擎，也是课程包能否启用的最终权威。课程包只描述教学内容和调用参数，不携带 Kotlin、JavaScript 或其他可执行代码，也不能借可视化调用访问 App 内部能力。

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
  "title": "数学七年级上册",
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
  "title": "有理数",
  "sections": []
}
```

小节字段：

```json
{
  "id": "section-01",
  "title": "正数和负数",
  "lessons": []
}
```

课时字段必须且只能是：

```json
{
  "id": "positive-negative-intro",
  "title": "为什么需要负数",
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
  "label": "教材1—2页",
  "pageStart": 1,
  "pageEnd": 2
}
```

`pageStart <= pageEnd <= textbook.pdf.pageCount`。

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

`expression` 保存纯 LaTeX 数学内容，不添加 `$...$`、`\\(...\\)` 等分隔符。

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

这四个字段必须完整且不能增加第五个字段。

`parameters` 只接受：

- JSON number；
- JSON boolean；
- 只含 number 的 JSON array。

不接受：

- string 参数；
- object / map；
- null；
- URL；
- 文件路径；
- 资源 ID；
- 任意代码或表达式；
- 混合类型数组。

`texts` 的 value 只能是字符串，而且只作为显示文本使用，不解释成路径、命令或数据源。

每个 renderer 有独立严格 schema：未知 renderer、未知参数、未知文本、缺少 required 字段或类型错误都会使课程包校验失败。

完整 renderer 规则见 `docs/VISUALIZATION_FRAMEWORK.md`。

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
  "analysis": ["方向相反使用负号"],
  "knowledgePointIds": ["positive-negative"],
  "difficulty": 1
}
```

要求：

- `difficulty` 为 1..5；
- 知识点 ID 必须存在；
- `analysis` 不能为空；
- 练习 ID 稳定且应保持唯一。

## 可视化与课程业务隔离

课程包不拥有绘图实现，也不能指定像素、Composable、Canvas 函数或任意 DSL。

课程层只表达：

```text
renderer = 用哪一种基础设施能力
parameters = 数学/几何/交互数值
texts = 教学显示文字
```

可视化层只消费这个不可执行调用对象。它不能访问：

- 网络；
- 文件系统；
- ContentResolver；
- 数据库/DataStore；
- AI；
- Repository/ViewModel；
- 课程下载器；
- 外部动态数据。

因此课程内容和渲染基础设施之间保持单向、可验证、可测试的边界。

## APK 启用前校验

课程进入 `active` 前至少完成：

1. 分发清单文件集合、大小和 SHA-256 校验；
2. `course.json` 根字段精确匹配；
3. 教材字段和 PDF 相对路径校验；
4. 知识点 ID、前置关系和循环依赖校验；
5. 章节、小节、课时关系校验；
6. 教材引用页码校验；
7. 教学步骤类型校验；
8. `visualization` renderer 和 schema 校验；
9. 练习知识点与难度校验；
10. 教材 PDF 实际存在；
11. 全部通过后才原子切换到 `active`。

任何旧 `scene`、未知 renderer 或越界可视化参数都不能通过安装校验。
