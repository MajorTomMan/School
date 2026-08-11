# School 课程数据契约

## 边界

APK 是唯一课程运行引擎，也是在设备上判断课程包是否可用的最终权威。课程包只描述课程业务，不声明协议版本、APK 版本、渲染器版本或能力版本，也不携带 Kotlin、JavaScript 或其他可执行代码。

`course.json` 根节点只有：

```json
{
  "textbook": {},
  "chapters": []
}
```

下载地址、文件大小和 SHA-256 不属于课程业务，统一放在包外的分发清单 `manifest.json` 中。APK 下载到暂存目录后，会再次校验文件、课程结构、场景参数、教材 PDF 与业务关系，全部通过后才启用。

课程包可以只包含原有讲解课程。需要答题、判题和掌握度时，必须同时增加：

```text
course.json
assessments.json
knowledge-points.json
assets/
```

`assessments.json` 与 `knowledge-points.json` 必须同时存在，不能只提供其中一个。这样支持新契约的 APK 仍可读取当前纯讲解课程，而带题目的新课程必须通过完整关系校验。

课程托管与 APK 发布相互独立。APK 默认不内置课程清单地址；只有构建或运行环境明确提供 `SCHOOL_COURSE_MANIFEST_URL`（或 Gradle 属性 `schoolCourseManifestUrl`）时，云端课程下载和更新检查才启用。未配置分发源时，APK 只读取设备上已经通过校验的本地课程。

## 教材

教材业务字段包括教材 ID、标题、出版社、版次、年级、学期、学科，以及 PDF 在课程缓存中的相对路径、页数和印刷页偏移。

```json
{
  "id": "pep-math-7-1",
  "title": "义务教育教科书·数学七年级上册",
  "publisher": "人民教育出版社",
  "edition": "人教版",
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

课程文件里不保存 PDF 下载地址、大小或摘要。

## 课程层级

课程由章节、小节、课程页组成。所有 ID 在一本教材内必须唯一且稳定。课程页通过 `sourcePage` 和可选的 `sourcePageEnd` 连接原教材，页面正文允许纵向滚动，教学环节之间允许横向翻页。

允许的课程页字段只有：

- `id`
- `title`
- `aliases`
- `sourcePage`
- `sourcePageEnd`
- `sourceReferences`
- `blocks`

`sourceReferences` 是可选的教材页内引用，用于把课程正文提到的教材图、表或题目示意图直接连回教材印刷页。例如：

```json
{
  "sourceReferences": [
    {
      "label": "图1.2-7",
      "sourcePage": 13
    }
  ]
}
```

`sourceReferences.sourcePage` 与课程页的 `sourcePage` 使用同一套教材印刷页码语义；APK 再结合 `textbook.pdf.pageIndexOffset` 定位实际 PDF 页。课程制作工具必须在人工查看原教材后记录引用页码，不能把 PDF 索引直接写入课程包。

作者校对记录、PDF 坐标、来源锚点和生成时间等内容不进入课程包。

## 内容块

APK 只接受以下内容块：

- `heading`：小标题
- `text`：教材原文、解释、历史说明、问题或图注
- `formula`：公式及成立条件
- `list`：知识点列表
- `example`：只展示教材例题、步骤与结果
- `exercise`：只展示题号、题干、选项与提示
- `conclusion`：本页结论
- `scene`：由 APK 渲染的交互或图示

`course.json` 中的 `example` 和 `exercise` 不保存标准答案，也不参与正确率和掌握度。需要用户独立作答的题目统一放在 `assessments.json`，避免展示内容与运行状态混在一起。

文本样式只有 `textbook`、`explanation`、`history`、`prompt`、`caption`。未知字段、未知块类型和错误数据不会被忽略或降级，而会使整个暂存课程安装失败。

## 场景

课程包通过 `scene.template` 选择 APK 内置的确定性场景，通过 `scene.data` 提供经过类型校验的业务参数。

```json
{
  "type": "scene",
  "template": "number_line",
  "data": {
    "mode": "value",
    "initial": 6.5
  }
}
```

场景参数使用真实 JSON 类型：数字是数字，开关是布尔值，数组是数组，不再使用字符串模拟类型。APK 会对每个模板分别检查允许字段、取值范围和元素关系。

`diagram` 是通用声明式图示，由线、箭头、点、圆、矩形、文字、折线和数轴等受限图元组成。它不能访问网络、文件、系统 API，也不能执行表达式或任意代码。

## 答题文件

`assessments.json` 根节点只有：

```json
{
  "courseId": "pep-math-7-1",
  "assets": [],
  "questionSets": [],
  "placements": []
}
```

`placements` 把题组放到 `course.json` 中已经存在的小节。每个题组必须且只能放置一次，不能成为孤立数据，也不能同时出现在多个小节。

每道题必须包含：

- 稳定的 `id` 和正整数 `revision`
- 题号 `number`
- 题干内容数组 `stem`
- 输入规格 `input`
- 声明式答案规则 `answer`
- 知识点权重 `knowledgeBindings`
- `0.0` 到 `1.0` 的难度 `difficulty`
- 提示 `hints`
- 单选项内容 `choices`
- 解题说明 `explanation`

首批输入与答案规则只有：

| 输入类型 | 答案规则 |
|---|---|
| `integer` | `exact_integer` |
| `decimal` | `decimal` |
| `rational` | `rational_equivalent` |
| `single_choice` | `single_choice` |
| `coordinate` | `coordinate` |

输入类型和答案规则必须匹配。分数与坐标使用明确的分子、分母对象，不使用需要执行的表达式。

题干、选项和解析共享以下声明式内容：

- `heading`
- `text`
- `formula`
- `list`
- `image`
- `table`
- `scene`

每道题在 App 中独立成页，但题干区域可以纵向滚动。图片、表格与原生场景可以混合使用。

## 图片和表格资产

`assessments.assets` 只保存业务引用与显示校验信息：

```json
{
  "id": "number-line-source",
  "path": "assets/figures/number-line-source.webp",
  "mediaType": "image/webp",
  "width": 1200,
  "height": 600
}
```

文件大小和 SHA-256 仍然只存在于分发清单。APK 会同时验证：

- 路径位于 `assets/` 且不能越界；
- 文件已在分发清单中声明；
- 下载大小和 SHA-256 正确；
- 图片真实格式与 `mediaType` 一致；
- 图片真实尺寸与课程声明一致；
- 单边尺寸和总像素数量不超过限制；
- 所有资产都被题目实际引用，不允许未声明或未使用资产。

简单表格使用结构化 `columns` 和 `rows`。需要保留教材原表时，可通过 `sourceAssetId` 关联裁剪图。复杂图表可以直接作为图片；数轴、坐标系和简单几何图优先使用 APK 原生 `scene`。

## 知识点文件

`knowledge-points.json` 根节点只有：

```json
{
  "courseId": "pep-math-7-1",
  "knowledgePoints": []
}
```

每个知识点包含：

- 稳定 ID；
- 标题和说明；
- 前置知识点 ID；
- 所属课程小节 ID。

APK 会拒绝不存在的前置知识、循环依赖、不存在的小节，以及题目中未声明的知识点引用。题目通过 `knowledgeBindings.weight` 表示对各知识点提供证据的相对权重。

## APK 校验

课程进入 `active` 前，APK 必须完成：

1. 更新清单只含允许字段，所有路径安全且 SHA-256 格式正确；
2. 下载大小和 SHA-256 与清单一致；
3. 解压后实际文件集合与清单声明完全一致；
4. `course.json` 只含允许的业务字段；
5. 教材、章节、小节、页面和内容块字段完整，ID 唯一；
6. 教材页码范围及页内教材引用页码有效；
7. 场景模板存在，参数类型、范围和图元关系有效；
8. 教材 ID 与更新清单一致；
9. PDF 文件头与页数正确；
10. 题目文件和知识点文件必须同时存在或同时不存在；
11. 题组、题目、选项、答案规则和知识点字段严格匹配；
12. 题组位置、知识点前置关系、课程小节和资产引用完整且无循环；
13. 图片实际格式、尺寸与像素上限正确；
14. 暂存目录通过全部检查后再原子切换到 `active`，失败时保留旧缓存。
