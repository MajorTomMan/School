# AGENTS.md

本文件是 School 仓库中 Agent、子代理和自动化开发助手需要遵守的唯一长期项目约束。仓库不维护 `docs/`、课程规范 README、长期 `tools/` 目录或其他平行规则文档；新增或修改长期约束只更新本文件。

## 1. 仓库定位

School Git 仓库只维护 App 本体以及 App 构建、测试、签名、更新和发布直接需要的文件。

允许长期存在的内容包括：

- `app/`：Android App。
- `visualization/`：受限的语义可视化基础设施。
- `.github/workflows/`：仅 App CI/CD。
- `signing/`：App 开发版签名和更新清单验证所需公开材料。
- `scripts/`：只有与 App 构建、运行或发布直接相关且确有长期价值的脚本，例如 App 更新推送脚本。
- `version.properties`、`.release-notes/current.md`、Gradle 配置和本文件。

禁止把以下内容作为长期仓库资产：

- `courses/` 或任何 authored course package、教材 PDF、课程 ZIP、课程发布产物。
- `tools/` 目录。
- 为一次任务临时编写的转换器、迁移器、抓取器、生成器、validator、审校脚本、发布脚本、数据修补脚本。
- 课程内容 CI、课程发布 CI、教材专属 CI。
- 与 App 无直接运行、构建或发布关系的辅助工程。

不要因为“以后可能有用”“方便重复执行”就把临时工具提交到仓库。需要临时处理数据、课程、教材或迁移时，在 Agent 当前执行环境或临时工作目录中完成，任务结束后不提交这些工具。只有明确属于 App 产品实现或 App 发布基础设施的代码才能进入仓库。

## 2. Git 与交付

- `dev` 是唯一日常开发和集成分支；功能、重构、规则调整均直接提交到 `dev`。
- 普通开发不创建独立 feature branch，也不要求 feature PR。
- `master` 是稳定主分支，不直接进行日常开发。
- 阶段性工作完成并确认可交付后，将 `dev` 合并到 `master`；交付方向固定为 `dev → master`。
- 不允许其他开发分支绕过 `dev` 直接进入 `master`。
- 合并前检查 `dev`、`master` 最新 head、差异和 App CI 状态。
- 普通 App 开发提交不要求每次提升版本号；准备发布时才由 Agent 统一维护版本元数据。

## 3. App CI/CD

GitHub Actions 只服务 App。

- 只监听 `dev` 的 App、Visualization、Gradle、签名、版本和 workflow 等 App 构建相关变化。
- `courses/**` 不存在，也不得重新加入 CI 触发路径。
- CI/CD 负责机械工作：解析版本、执行单元测试、编译 APK、生成并签名更新清单、上传产物、刷新 GitHub Development Release。
- CI/CD 不负责决定版本号，不生成版本号，不编写或审核变更点，不判断课程内容正确性。
- 不为了 CI 新建 Python validator 或其他长期工具。

`dev-latest` tag 是上一已发布开发版的唯一版本事实源。自动发布必须同时满足：

1. 当前 `VERSION_NAME` 与 `dev-latest` 不同，且当前 `VERSION_CODE` 高于 `dev-latest`。
2. `dev-latest..HEAD` 存在 App、Visualization 或 App 构建相关代码变化。

只有代码变化但版本未提升时，只测试和构建，不发布；只有版本变化但没有 App 代码变化时，也不发布。

## 4. App 版本与发布说明

- `version.properties` 是 App 唯一版本来源。
- Agent 准备发布前必须读取 `dev-latest:version.properties`、`dev-latest:.release-notes/current.md`，并比较 `dev-latest..dev` 的 App 变化。
- `VERSION_CODE` 必须递增；`VERSION_NAME` 使用 `x.y.z`。
- `.release-notes/current.md` 只描述“上一已发布版 → 本次待发布版”，不是累计 changelog。
- 发布说明统一使用：

```markdown
## 变更点

- 简洁变更点。
- 简洁变更点。
```

- 每次发布重新编写变更点；禁止复制、继承或重复上一版本已经出现过的条目。
- 多个同类小改动合并成一条，不逐条翻译 commit，不为了数量写空泛内容。
- 课程内容更新不属于 App 版本变化，不写入 App Release notes；只有 App 对课程能力或课程分发机制本身发生变化时才属于 App 变更点。

## 5. 课程分发架构

课程内容是独立内容资产，不存放在 School Git 仓库，也不通过 App CI/CD 发布。

课程发布服务固定为：

```text
https://course.flashnamesl.workers.dev
```

公开地址：

```text
Testing manifest:
https://course.flashnamesl.workers.dev/cloud/course/public/testing/manifest.json

Stable manifest:
https://course.flashnamesl.workers.dev/cloud/course/public/stable/manifest.json

Immutable release objects:
https://course.flashnamesl.workers.dev/cloud/course/public/releases/<release-id>/...
```

正式 App 默认只消费 `stable/manifest.json`。`testing/manifest.json` 只用于新课程发布后的验证，不作为正式默认入口。App 可通过明确的构建环境配置临时覆盖课程 manifest 地址，但默认稳定地址不得依赖外部注入才能工作。

课程 Worker 的发布接口为：

```text
GET  /cloud/course/object?path=<object-path>
POST /cloud/course/upload-url
PUT  <signed-upload-url>
POST /cloud/course/upload-complete
POST /cloud/course/channel/publish
```

发布鉴权使用 `Authorization: Bearer <COURSE_API_TOKEN>`。Token 只存在于受控执行环境或秘密存储中，禁止写入仓库、课程包、日志或提交记录。

## 6. 课程包发布流程

课程制作和发布由 Agent / 子代理在仓库外的临时工作区完成，不提交课程源文件或发布工具。

标准流程固定为：

1. 读取对应教材和 App 当前课程运行契约，在临时工作区编写或更新 authored course。
2. 完成结构、知识点、术语、例题答案、练习答案、可视化 invocation 和教材对照审校。
3. 生成一个唯一且不可变的 `release-id`。建议使用只包含字母、数字、点、下划线和短横线的可追踪标识；同一 `release-id` 一经发布不得覆写为另一份内容。
4. 在临时目录生成 release artifact。根 `manifest.json` 必须遵守 App 当前真实分发契约；其中课程 package、课程文件和教材资源 URL 必须全部指向：

```text
https://course.flashnamesl.workers.dev/cloud/course/public/releases/<release-id>/...
```

5. 对每个文件计算 size 和 SHA-256；通过 `/cloud/course/upload-url` 获取签名上传地址，上传完成后调用 `/cloud/course/upload-complete`。
6. 确认 `releases/<release-id>/manifest.json` 已存在且 SHA-256 正确。
7. 调用 `/cloud/course/channel/publish`，先将该 release 发布到 `testing`。
8. 从公开 Testing 地址重新下载 manifest，并逐项检查 manifest、文件 URL、大小、SHA-256、课程解析和关键内容；验证必须针对远端实际资源，而不是只检查本地生成目录。
9. Testing 验证通过后，再调用 `/cloud/course/channel/publish` 将同一个 immutable release 提升到 `stable`。
10. 从 Stable 地址重新下载并确认最终 manifest 与已验证的 release 一致。Stable 切换完成即视为课程发布完成，不需要重新构建 App。

禁止跳过 Testing 直接替换 Stable；禁止修改已经发布的 immutable release 内容；修复课程时发布新的 `release-id`，重新走 Testing → Stable。

如果当前 Agent 执行环境没有 Worker 写权限或 `COURSE_API_TOKEN`，不得在仓库里临时创建发布 CI 或提交发布脚本来绕过限制；应明确报告缺少的发布能力，由具备授权的环境继续执行。

## 7. 数字课本编写原则

课程目标不是教材摘要、知识卡片或教材旁边的 App 解说，而是基于教材知识体系重新编写一套可以独立阅读和学习的严肃数字课本。

- 教材负责准确，App 负责理解。
- 以对应教材的知识顺序、术语、概念边界、论证路径、教学语气和详细程度为基线。
- 不大段复制教材原文；教材材料与我们自己的严谨表述有机结合。
- “先想一想”之前必须已有明确的情境、事实、例子、图示或已有知识；不能让问题本身承担背景介绍职责。
- 推荐节奏是“情境或例子 → 严谨叙述 → 观察/可视化 → 先想一想 → 概念形成或推导 → 例题 → 检查一下 → 记住 → 练习”，但服从教材真实编排，不机械套模板。
- “记住”用于完整收束定义、法则、条件、结论和必要注意事项，不写成口号式速记卡。
- 教材静态数学图、示意图、数轴、关系图和可交互模型，只要能够准确语义化，就优先使用 App Renderer 重建；保留教学意义、关键标注和信息层次，不复制出版社版式或截图。
- 教材例题和情境可以作为教学路径基线，但正文、问题和解析应重新组织，不能为了“原创”擅自改变知识难度。

## 8. 课程与 App 运行边界

- APK 提供课程运行能力；课程包只描述教学内容和受限语义调用。
- 课程包不得携带或执行 Kotlin、JavaScript、Python、Shell 或其他任意代码。
- 课程包不得通过反射、回调、类名、URL、文件路径或任意脚本获得宿主能力。
- App 对远端课程执行必要的运行时格式、安全、完整性和哈希校验；这是 App 产品能力，不是课程 CI。
- 课程更新和 App 发布完全解耦；课程 Stable manifest 更新后，已有 App 应可直接获取新课程。

## 9. Visualization 架构

- Visualization 是独立基础设施，不是课程业务逻辑。
- 课程只通过语义 invocation 调用：

```json
{
  "type": "visualization",
  "renderer": "...",
  "parameters": {},
  "texts": {}
}
```

- 共享的是绘制能力，不把所有知识点塞进万能 Renderer。
- Renderer key、参数和文本必须具有明确教学语义。
- 禁止任意代码、反射、动态类、回调和脚本执行。
- 数学表达式只进入明确允许的安全 Parser/AST 字段。
- `:visualization` 不访问网络、文件、数据库、DataStore、AI、Repository、ViewModel 或课程下载器。
- 课程层不直接实现 Canvas 或像素算法；缺能力时补充可复用的语义 Renderer。

## 10. Verification 与学科引擎

- Verification 共享输入、结果、结构化步骤、问题/警告和可视化请求，但不实现万能 Solver。
- `VerificationStep` 表示结构化规则变换，不退化成不可验证的字符串列表。
- Math、Physics、Chemistry、Biology 等拥有独立领域语义；可以复用底层数学能力，但不能混淆学科规则。
- Math Engine 不依赖 Visualization；App/UI 层负责把领域结果映射为可视化 invocation。
- 不使用 `Any`、无类型 Map、反射、脚本或任意回调作为跨学科协议。
- 本地验证没有 AI fallback；不支持时明确返回不支持。
- 当前 Math Engine 范围是初高中基础数学，不把极限、导数、积分、微分方程、Taylor 展开等高等数学混入当前本地验证。
- 英语、日语的语言验证需要形态、语法、句法、语义槽位和上下文模型；语文最后设计，开放写作和文学评价不伪装成确定性规则验证。

## 11. 代码与工具规则

- 优先删除旧实现，不长期保留无业务价值的兼容壳、alias、历史命名、退役 wrapper 和墓碑测试。
- 新架构稳定后不要为了旧调用继续增加兼容分支。
- 不引入无必要的 God class、万能 Renderer、万能 Solver 或万能协议。
- 领域内核尽量保持纯 Kotlin，避免依赖 Android、网络、数据库、AI 或 UI。
- App/UI 负责组合领域结果和基础设施，不反向向领域层注入 UI 依赖。
- 修改前检查真实引用；删除后通过 App 编译和单元测试确认依赖闭合。
- Kotlin、Java、XML 不要出现无意义换行；方法调用、参数列表和表达式在可读前提下保持紧凑。
- 不为了格式化制造大面积无关 diff。
- 禁止为了方便在仓库中新建 `tools/`。任何临时脚本默认只属于当前 Agent 执行环境。
- `scripts/` 也不是杂物目录：只有与 App 构建、运行、签名、更新或发布直接相关且长期需要的脚本才能提交；教材、课程、数据整理、转换、抓取、审校和迁移类脚本不得长期留在这里。

## 12. 文档规则

- 不恢复 `docs/`。
- 不新增架构 README、课程规范 README、Foundation 文档或平行规则文件。
- 长期项目规则只写 `AGENTS.md`。
- `.release-notes/current.md` 是当前待发布 App 的发布元数据，不是项目文档。
- 实现细节优先通过类型、测试和清晰命名表达，避免依赖大量旁路文档。
