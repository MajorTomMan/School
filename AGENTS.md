# AGENTS.md

本文件是 School 仓库中 Agent / 子代理 / 自动化开发助手需要遵守的唯一项目约束文档。不要重新建立 `docs/` 架构、流程或课程规范文档；新的长期约束统一更新到本文件。

## 1. Git 与交付

- `dev` 是功能集成分支。
- 所有功能、重构和课程内容修改都使用独立 feature branch，并通过 Pull Request 合入 `dev`。
- 不直接向 `dev` 推送功能修改。
- 未经用户明确要求，不主动合并 PR。
- 合并前检查当前 PR head，避免基于过期状态作结论。
- App 代码变化遵守当前版本号和中文 release notes 规则；课程内容变化不要求提升 App 版本。

## 2. CI/CD 边界

- GitHub Actions 只服务 App 自身。
- `.github/workflows/` 只保留 App 编译、App 测试、App 架构检查、APK/更新包构建与 App 发布所需 workflow。
- `courses/**` 变化不得触发 App CI/CD。
- App CI/CD 不负责课程包的教材审校、答案核对、课程 schema 审核、打包、上传或发布。
- 不新增 Course CI、教材专属 CI、课程发布 CI。
- App 自身的测试与架构检查属于 CI 范围，例如 visualization isolation 和 Kotlin/Android 单元测试。

## 3. 课程包属于内容工程

- 课程包与 App 构建链路解耦。
- 课程内容的准确性由 Agent / 子代理、人工复核或其他独立内容流程负责，不依赖 GitHub Actions validator。
- 仓库不维护课程内容 validator、教材专属 validator、课程 schema 校验脚本或 Python 版 visualization contract 镜像。
- 需要审校课程时，由 Agent / 子代理直接读取课程包和对应教材，按当前 App 真实运行契约检查结构、知识点、答案、语言和可视化调用；不要为了审校重新创建长期 validator 工具。
- 仓库中的课程打包/发布脚本如果仍有实际用途可以保留，但不得承担教材内容正确性判断，也不得挂入 App CI/CD。
- 课程包的 authored source 保持可读、可 diff；当前课程使用 `courses/<textbook-id>/course.json`。
- 不恢复 gzip/base64 分卷 authored source，不恢复旧 page-generated course、scene 兼容层或迁移器。
- 教材负责准确，App 负责理解。课程编排、概念边界、术语和教学顺序应以对应教材为基线，App 可以改善交互、步骤和可视化，但不能擅自改写知识体系。
- 课程正文不要长段复制教材原文；应在保持教材术语、顺序和教学语气的前提下重新组织表达。

## 4. 课程与 App 的运行边界

- APK 是课程运行能力的实现方；课程包只描述教学内容和语义调用。
- 课程包不得携带或执行 Kotlin、JavaScript、Python、Shell 或其他任意代码。
- 课程包不得通过反射、回调、类名、URL、文件路径或任意脚本访问宿主能力。
- App 对下载后的课程包仍应执行必要的运行时安全/完整性校验；这属于 App 功能，不是课程内容 CI。

## 5. Visualization 架构

- Visualization 是独立基础设施，不是课程业务逻辑。
- 课程只通过语义 invocation 调用可视化：

```json
{
  "type": "visualization",
  "renderer": "...",
  "parameters": {},
  "texts": {}
}
```

- 共享的是绘制能力，不把所有知识点塞进万能 Renderer。
- Renderer key、参数类型和显示文本必须具有明确语义；不允许任意代码、反射、回调或动态类执行。
- 数学表达式只能进入明确支持的安全数学表达式字段，由受限 Parser/AST 处理；不能把表达式字符串当脚本。
- `:visualization` 不访问网络、文件、数据库、DataStore、AI、Repository、ViewModel 或课程下载器。
- 课程层不直接实现 Canvas、像素算法或渲染类。

## 6. Verification 架构

- Verification 使用共享基础协议/步骤模型，但不实现一个万能 Solver。
- 公共层共享输入、结果、结构化步骤、问题/警告和可视化请求；学科规则留在各自 Engine。
- `VerificationStep` 应表示结构化的规则变换，不把做题过程退化成不可验证的 `List<String>`。
- 数学、物理、化学、生物共享验证基础设施，但拥有独立领域语义。
- Math Engine 不依赖 Visualization；由 App/UI 层把数学结果转换为可视化 invocation。
- 不使用 `Any`、无类型 Map、反射、脚本或任意回调作为跨学科协议。
- 本地验证不存在 AI fallback；本地引擎不支持的内容应明确返回不支持。

## 7. 数学范围

- 当前本地 Math Engine 目标范围是初中和高中基础数学。
- 优先支持：数值表达式、分数/根式/幂、整式、方程、不等式、函数、数列、解析几何、基础三角、概率统计等中学内容。
- 不把极限、导数、微分、积分、微分方程、Taylor 展开、无穷级数等高等数学混入当前本地验证范围。
- 数学步骤必须由显式规则驱动，并尽可能验证每一步变换的合法性。

## 8. 理化生扩展原则

- Physics Engine 可复用 Math Engine 的表达式、方程和计算能力，但物理定律、单位和量纲属于 Physics Engine。
- Chemistry Engine 拥有化学式解析、方程配平、守恒和化学计量语义；数值计算可以复用 Math Engine。
- Biology Engine 只对确定性、结构化主题进行本地规则验证，例如基础遗传、概率和关系模型；不要把开放型生物问答伪装成确定性验证。

## 9. 语言学科顺序

- 英语和日语的本地语言验证框架晚于数理化生稳定后再推进。
- 语言验证不能只做字符串精确匹配，应区分形态、语法、句法、语义槽位和上下文。
- 语文最后设计。语文同时包含现代汉语、古汉语、逻辑、文学与写作，不能强行套进单一 Boolean 验证模型。
- 作文优美程度、开放文学赏析等主观任务不进入本地确定性验证；如未来需要，可作为独立 AI 能力处理，不污染本地 Rule Engine。

## 10. 代码与架构约束

- 优先删除旧实现，不保留无业务价值的兼容壳、alias、V2 历史命名、退役 wrapper 和墓碑测试。
- 新架构稳定后不要为了旧调用继续增加兼容分支。
- 不引入无必要的万能抽象、God class 或万能 Renderer/Solver。
- 领域内核尽量保持纯 Kotlin，避免依赖 Android、网络、数据库、AI 或 UI。
- App/UI 负责组合领域结果和基础设施，不把 UI 依赖反向注入领域引擎。
- 修改前检查真实引用；删除旧代码后以 App 编译和单元测试验证依赖闭合。

## 11. 代码风格

- Kotlin/Java/XML 不要出现无意义的换行；方法调用、参数列表和表达式在可读前提下保持紧凑。
- 不为了“格式化”制造大面积无关 diff。
- 新代码优先清晰的显式控制流，避免为了炫技引入难维护的抽象。

## 12. 文档规则

- 不恢复 `docs/` 文档体系。
- 项目长期约束只写在根目录 `AGENTS.md`。
- 不额外维护架构 README、课程规范 README 或 Foundation 文档。
- `.release-notes/current.md` 是 App 发布元数据，不视为项目架构文档，继续由 App CI/CD 使用。
- 代码应尽量自解释；实现细节优先通过类型、测试和清晰命名表达，而不是依赖大量旁路文档。
