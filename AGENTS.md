# AGENTS.md

本文件是 School 仓库中 Agent、子代理和自动化开发助手需要遵守的唯一长期项目约束。仓库不维护 `docs/`、课程规范 README、长期 `tools/` 目录或其他平行规则文档；新增或修改长期约束只更新本文件。

## 1. 仓库定位

School Git 仓库只维护 App 本体以及 App 构建、测试、签名、更新、发布直接需要的文件，以及固定的课程 R2 管理基础设施。

允许长期存在的内容包括：

- `app/`：Android App。
- `visualization/`：受限的语义可视化基础设施。
- `.github/workflows/`：App CI/CD，以及唯一的手动 R2 存储管理 workflow `.github/workflows/r2-storage-manager.yml`。
- `signing/`：App 开发版签名和更新清单验证所需公开材料。
- `scripts/`：与 App 构建、运行或发布直接相关且确有长期价值的脚本，以及固定的课程 R2 管理器 `scripts/course_r2_manager.py`。
- `version.properties`、`.release-notes/current.md`、Gradle 配置和本文件。

禁止把以下内容作为长期仓库资产：

- `courses/` 或任何 authored course package、教材 PDF、课程 ZIP、课程发布产物。
- `tools/` 目录。
- 除 `scripts/course_r2_manager.py` 外，为一次任务临时编写的转换器、迁移器、抓取器、生成器、validator、审校脚本、发布脚本、数据修补脚本。
- 课程内容 CI、课程发布 CI、教材专属 CI。
- 与 App 无直接运行、构建或发布关系的辅助工程。

不要因为“以后可能有用”“方便重复执行”就把临时工具提交到仓库。需要临时处理数据、课程、教材或迁移时，在 Agent 当前执行环境或临时工作目录中完成，任务结束后不提交这些工具。课程 R2 的文件、目录、release 和 channel 管理统一复用 `scripts/course_r2_manager.py`，不得再创建平行 R2 管理脚本。

## 2. Git 与交付

- `dev` 是唯一日常开发和集成分支；功能、重构、规则调整均直接提交到 `dev`。
- 普通开发不创建独立 feature branch，也不要求 feature PR。
- `master` 是稳定主分支，不直接进行日常开发。
- 阶段性工作完成并确认可交付后，将 `dev` 合并到 `master`；交付方向固定为 `dev → master`。
- 不允许其他开发分支绕过 `dev` 直接进入 `master`。
- 合并前检查 `dev`、`master` 最新 head、差异和 App CI 状态。
- 普通 App 开发提交不要求每次提升版本号；准备发布时才由 Agent 统一维护版本元数据。

## 3. App CI/CD 与 R2 管理

GitHub Actions 默认只服务 App；唯一额外允许的是手动触发的 R2 存储管理 workflow `.github/workflows/r2-storage-manager.yml`。

R2 管理 workflow 固定遵守：

- 只允许 `workflow_dispatch` 手动触发，不监听 push、pull request、tag、schedule 或其他自动事件。
- 只复用 `scripts/course_r2_manager.py`，可执行课程对象/目录 CRUD、上传仓库外已经制作完成的 immutable course release、手动切换 Testing/Stable channel，以及在明确确认后清空课程存储；不得生成课程、审校课程、修改课程正文或承担教材专属处理。
- 普通课程对象/目录 CRUD 默认使用 `worker` backend，通过 `COURSE_BASE_URL` 与 Actions Secret `COURSE_API_TOKEN` 调用课程 Worker；这也是 `list`、`read`、`create`、`update`、`delete`、目录管理和 `purge` 的默认路径。
- `direct` backend 只作为底层 R2 恢复/诊断备用路径。只有明确选择 `backend=direct` 时才使用 Repository Variables `R2_ACCOUNT_ID`、`R2_ACCESS_KEY_ID`、`R2_BUCKET_NAME` 与 Actions Secret `R2_SECRET_ACCESS_KEY`，且不得把 direct 作为课程日常管理的默认依赖。
- Worker 侧删除必须由 Cloudflare Worker 环境变量 `COURSE_ALLOW_DELETE=true` 显式开启；该开关不是 GitHub Secret，也不写入仓库。若未开启，删除操作应明确报告 Worker 的 `delete_disabled`，不得绕过保护。
- `purge` 只允许 `worker` backend，作用域受 Worker 的 `COURSE_PREFIX` 限制；必须同时显式确认删除并开启 `allow_release_mutation`，完成后必须重新列举并确认课程存储为空。禁止提供 direct bucket 级 purge，避免误清共享 bucket 的非课程对象。
- `release-upload` / `publish` 始终使用 Worker；`release-upload` 只允许 `none` 或 `testing`，不得直接发布 Stable；Testing 验证完成后才允许通过单独的 `publish` 操作提升同一 immutable release 到 Stable。
- 删除操作必须显式确认；`releases/` 下 immutable 对象的 update/delete 默认禁止，只有明确恢复时才允许显式开启 `allow_release_mutation`。
- 文件 create/update 可从临时 HTTP(S) URL 获取源文件；目录 create/update 与 `release-upload` 从 ZIP 获取源目录或 release artifact。Worker backend 不创建空的 R2 “目录”，目录必须至少包含一个真实对象。不要把长期凭据写入 workflow input 或 source URL。

App CI/CD 规则保持：

- App CI 只监听 `dev` 的 App、Visualization、Gradle、签名、版本和 workflow 等 App 构建相关变化。
- `courses/**` 不存在，也不得重新加入 CI 触发路径。
- CI/CD 负责机械工作：解析版本、执行单元测试、编译 APK、生成并签名更新清单、上传产物、刷新 GitHub Development Release。
- Development Release 的阻断型单元测试只覆盖 App 数据结构/协议与数学引擎；另以 `:app:assembleDebug` 验证 APK 能正常构建。
- 阻断型单元测试不得依赖课程包、课程正文、教材内容、远端课程、UI/Presentation/Visualization 展示文案；数学表达式、协议 key、结构化状态和规则 ID 不视为展示文案。
- 其他 UI、课程、网络等专项测试如保留，仅用于本地或专项验证，不阻断 Development Release。
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

课程 Worker 的受控管理接口为：

```text
GET    /cloud/course/objects?prefix=<prefix>&limit=<limit>&cursor=<cursor>
GET    /cloud/course/object?path=<object-path>
POST   /cloud/course/upload-url
PUT    <signed-upload-url>
POST   /cloud/course/upload-complete
POST   /cloud/course/download-url
GET    <signed-download-url>
POST   /cloud/course/channel/publish
DELETE /cloud/course/object
```

除签名上传/下载 URL 与公开分发地址外，管理接口鉴权使用 `Authorization: Bearer <COURSE_API_TOKEN>`。Token 只存在于受控执行环境或秘密存储中，禁止写入仓库、课程包、日志或提交记录。`DELETE /cloud/course/object` 使用 JSON body，至少包含完全一致的 `path` 与 `confirm`；管理器应在删除前读取 metadata，并尽量同时提交 `expected_etag`、`expected_size` 防止对象在确认后发生变化。Worker 删除还要求 Cloudflare 环境变量 `COURSE_ALLOW_DELETE=true`。

课程 R2 管理统一使用 `scripts/course_r2_manager.py`。普通课程管理默认 backend 为 `worker`；配置解析顺序固定为：命令行参数 → 当前环境变量 → GitHub Repository Variables → 内置默认值。Worker 非敏感配置使用 Repository Variable `COURSE_BASE_URL`，敏感配置 `COURSE_API_TOKEN` 必须使用受控环境变量或 GitHub Actions Secret。只有显式 `--backend direct` 时才读取 `R2_ACCOUNT_ID`、`R2_ACCESS_KEY_ID`、`R2_BUCKET_NAME` 与 `R2_SECRET_ACCESS_KEY`；其中 `R2_SECRET_ACCESS_KEY` 必须使用受控环境变量或 Secret，不得存入可直接读取的 Repository Variables。

固定的 `.github/workflows/r2-storage-manager.yml` 是 `scripts/course_r2_manager.py` 的手动受控执行入口：默认通过 Worker 做课程文件/目录 CRUD，也可上传仓库外已完成制作与校验的 release artifact，并按标准流程手动发布 Testing/Stable；只有明确选择 direct backend 时才直接访问 R2。它不生成、不审校、不保存课程源，因此不属于课程内容生成 CI。

## 6. 课程包发布流程

课程制作由 Agent / 子代理在仓库外的临时工作区完成；课程源文件和发布产物不提交到仓库。课程 R2 上传、查询、修改、删除、目录管理、release 上传和 channel 发布统一调用仓库中的 `scripts/course_r2_manager.py`，默认通过 Worker 完成，不再临时生成发布脚本；只有底层恢复/诊断需要时才显式使用 `--backend direct`。

标准流程固定为：

1. 读取对应教材和 App 当前课程运行契约，在临时工作区编写或更新 authored course。
2. 完成结构、知识点、术语、例题答案、练习答案、可视化 invocation 和教材对照审校。
3. 生成一个唯一且不可变的 `release-id`。建议使用只包含字母、数字、点、下划线和短横线的可追踪标识；同一 `release-id` 一经发布不得覆写为另一份内容。
4. 在临时目录生成 release artifact。根 `manifest.json` 必须遵守 App 当前真实分发契约；其中课程 package、课程文件和教材资源 URL 必须全部指向：

```text
https://course.flashnamesl.workers.dev/cloud/course/public/releases/<release-id>/...
```

5. 使用 `python scripts/course_r2_manager.py release-upload --root <release-root> --release-id <release-id> --channel testing` 完成 size/SHA-256 校验、签名上传、完成确认并发布到 Testing；同内容对象可跳过，不允许覆盖已存在但内容不同的 immutable release 对象。若当前执行环境没有 `COURSE_API_TOKEN`、但仓库 Actions Secret 已配置，可使用固定的 `r2-storage-manager.yml` 选择 `release-upload`，输入同一 release artifact 的临时 HTTPS ZIP 地址、release ID 和 `testing` channel 执行同一管理器。
6. 确认 `releases/<release-id>/manifest.json` 已存在且 SHA-256 正确。
7. 从公开 Testing 地址重新下载 manifest，并逐项检查 manifest、文件 URL、大小、SHA-256、课程解析和关键内容；验证必须针对远端实际资源，而不是只检查本地生成目录。
8. Testing 验证通过后，使用 `python scripts/course_r2_manager.py publish --release-id <release-id> --channel stable` 将同一个 immutable release 提升到 Stable；若由 GitHub Actions 执行，则使用固定 R2 manager workflow 的 `publish` 动作，且必须与已验证的 Testing release ID 完全相同。
9. 从 Stable 地址重新下载并确认最终 manifest 与已验证的 release 一致。Stable 切换完成即视为课程发布完成，不需要重新构建 App。

禁止跳过 Testing 直接替换 Stable；禁止修改已经发布的 immutable release 内容；修复课程时发布新的 `release-id`，重新走 Testing → Stable。

如果当前 Agent 执行环境没有 Worker 写权限，应优先检查固定 `r2-storage-manager.yml` 是否可通过仓库已配置的 `COURSE_API_TOKEN` Actions Secret 执行对应操作；不得再创建新的平行发布 CI、替代脚本，也不得把 Secret 降级存入 Repository Variables。只有明确执行 direct 恢复操作时才检查 R2 直连凭据；若所选 backend 在本地环境和固定 workflow 中都没有所需授权，再明确报告缺少的授权。

## 7. 课程包格式规范

本节是 authored course 与远端 release artifact 的长期标准模板。真实约束仍以 App 当前 parser、staging validator 和下载器代码为最终事实源；修改课程运行契约时必须同步更新本节，禁止让长期说明与运行时代码分叉。

### 7.1 Release 目录与文件职责

课程制作和发布产物只存在于仓库外临时工作区以及 R2 immutable release 中。推荐 release artifact 结构：

```text
<release-root>/
├── manifest.json
└── courses/
    └── <course-id>/
        ├── package/
        │   └── <course-id>.zip
        └── assets/
            ├── textbook.pdf
            └── ...
```

职责固定为：

- `manifest.json`：整个 release 的远端索引，描述教材 ID、完整 ZIP、最终安装文件集合、URL、文件大小、SHA-256 和 `bundled` 状态。
- `<course-id>.zip`：只携带 `manifest.json` 中该教材 `bundled=true` 的文件，不能多文件也不能少文件。
- `course.json`：课程主体，必须位于最终教材安装目录根部，且必须声明为 `bundled=true`。
- `assets/textbook.pdf`：教材 PDF。推荐作为 `bundled=false` 的独立对象，避免仅修改课程正文时重复传输大 PDF。
- `assessments.json`、`knowledge-points.json`：可选正式题库契约；两者必须同时存在或同时不存在。
- `assets/...`：题目图片等静态资源。大资源推荐 `bundled=false` 独立存储。
- `.course-state.json`：App 安装后自行生成的本地状态文件，课程 release 和 ZIP 中禁止提供。

最终安装目录由 App 组合 ZIP 和外部文件得到，而不是要求 ZIP 自己包含全部资源：

```text
course-packs/active/<course-id>/
├── course.json
├── assets/
│   └── textbook.pdf
├── assessments.json          # 可选
├── knowledge-points.json     # 可选
└── .course-state.json        # App 生成
```

### 7.2 `manifest.json` 契约

顶层只允许 `textbooks`，每本教材只允许 `id`、`package`、`files`。不要加入未被 App parser 接受的描述字段。

结构模板：

```json
{
  "textbooks": [
    {
      "id": "pep-math-7-1",
      "package": {
        "path": "courses/pep-math-7-1/package/pep-math-7-1.zip",
        "url": "https://course.flashnamesl.workers.dev/cloud/course/public/releases/example-release/courses/pep-math-7-1/package/pep-math-7-1.zip",
        "size": 12345,
        "sha256": "0000000000000000000000000000000000000000000000000000000000000000"
      },
      "files": [
        {
          "path": "course.json",
          "url": "",
          "size": 2345,
          "sha256": "1111111111111111111111111111111111111111111111111111111111111111",
          "bundled": true
        },
        {
          "path": "assets/textbook.pdf",
          "url": "https://course.flashnamesl.workers.dev/cloud/course/public/releases/example-release/courses/pep-math-7-1/assets/textbook.pdf",
          "size": 12345678,
          "sha256": "2222222222222222222222222222222222222222222222222222222222222222",
          "bundled": false
        }
      ]
    }
  ]
}
```

上例的大小和 SHA-256 只展示字段形状，不是可发布值。发布前必须根据实际对象重新计算。

硬约束：

- `textbooks` 必须非空，教材 `id` 不能重复。
- manifest 教材 ID 使用 `[A-Za-z0-9._-]+`；为了同时兼容可选 Assessment 契约，实际 authored course ID 优先使用小写字母开头的 `[a-z][a-z0-9_-]{0,95}`。
- `package.path` 必须是安全相对路径并以 `.zip` 结尾；`package.url` 不能为空；`package.size > 0`。
- 所有 SHA-256 都必须是 64 位十六进制字符串。
- `files` 必须非空，路径不得重复；每项必须完整声明 `path`、`url`、`size`、`sha256`、`bundled`，且 `size > 0`。
- `course.json` 必须出现在 `files` 中且 `bundled=true`。
- `bundled=false` 的文件必须提供非空下载 URL。
- 文件路径必须使用正斜杠相对路径，不允许绝对路径、空路径段、`.` 或 `..`。
- `.course-state.json` 是 APK 保留路径，课程不得占用。

### 7.3 ZIP 与 `bundled` 规则

`bundled` 表示文件是否物理存在于完整 ZIP 中。完整安装时 App 解压 ZIP 后会比较：

```text
ZIP 实际文件集合 == manifest.files 中 bundled=true 的文件集合
```

因此：

- ZIP 中不能额外加入 `README`、缩略图、临时文件或其他未声明内容。
- `bundled=true` 的文件缺失会导致完整包安装失败。
- `bundled=false` 文件不应出现在 ZIP 中，由 App 独立下载、校验后组合到 staging。
- 推荐把 `course.json` 以及体积较小的结构化 JSON 放 ZIP，把 PDF、题目图片、未来的大型音视频资源作为独立文件。
- 若 `bundled=true` 文件后续变化但自身没有可用 URL，增量计划会退回完整 ZIP 更新；这是允许且符合当前设计的行为。
- ZIP 解压后的总文件体积不得超过 App 当前限制 2 GiB。

### 7.4 `course.json` 主体契约

顶层固定包含：

```text
textbook
knowledgePoints
chapters
```

最小结构模板：

```json
{
  "textbook": {
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
  },
  "knowledgePoints": [
    {
      "id": "positive-negative",
      "name": "正数和负数",
      "description": "表示相反意义的量",
      "prerequisiteIds": []
    }
  ],
  "chapters": [
    {
      "id": "chapter-01",
      "title": "有理数",
      "sections": [
        {
          "id": "section-01",
          "title": "正数和负数",
          "lessons": [
            {
              "id": "positive-negative-intro",
              "title": "为什么需要负数",
              "aliases": ["正数和负数"],
              "goals": ["理解相反意义的量"],
              "knowledgePointIds": ["positive-negative"],
              "prerequisiteLessonIds": [],
              "references": [
                {
                  "label": "教材1—2页",
                  "pageStart": 1,
                  "pageEnd": 2
                }
              ],
              "steps": [
                {
                  "type": "question",
                  "prompt": "低于0℃怎么表示？",
                  "hint": "想想方向"
                }
              ],
              "practice": [
                {
                  "id": "practice-01",
                  "prompt": "向西8米怎么表示？",
                  "answer": "-8米",
                  "analysis": ["方向相反使用负号"],
                  "knowledgePointIds": ["positive-negative"],
                  "difficulty": 1
                }
              ],
              "summary": ["正负号用于区分相反方向"]
            }
          ]
        }
      ]
    }
  ]
}
```

结构和引用规则：

- `textbook.id` 必须与对应 manifest 教材 `id` 完全一致。
- `knowledgePoints` 至少一个，ID 不得重复；所有 `prerequisiteIds` 必须存在且知识点依赖图不能成环。
- `chapters` 至少一个；每个 chapter 至少一个 section；每个 section 至少一个 lesson。
- lesson ID 在整本教材内唯一；`prerequisiteLessonIds` 必须引用存在课时且不能形成循环。
- 每个 lesson 的 `goals`、`knowledgePointIds`、`steps`、`summary` 必须非空；知识点绑定必须真实存在。
- `references` 页码使用正整数，必须满足 `pageStart <= pageEnd <= textbook.pdf.pageCount`。
- `practice.id` 在整本教材内唯一；`analysis`、`knowledgePointIds` 必须非空；`difficulty` 必须是 JSON 整数 `1..5`。
- integer 字段必须是真正的 JSON 整数；字符串 `"1"` 和小数 `1.0` 不作为整数兼容处理。
- parser 使用严格字段白名单；不要自行增加 `remoteUrl`、`scene` 或其他历史/临时字段。

### 7.5 教学步骤类型

`steps` 当前只接受：

```text
explanation
question
keyIdea
formula
example
visualization
checkpoint
summary
```

字段形状以 App 当前 `CourseDocumentParser` 为准。特别规则：

- `formula.expression` 保存不带 `$...$`、`\(...\)`、`\[...\]` 定界符的纯 LaTeX。
- 数学表达式不要混入中文说明，也不要用 `²`、`×`、`÷`、`≤`、`π` 等 Unicode 数学符号替代 LaTeX 命令。
- `visualization` 只允许调用 App 已注册 renderer；结构固定为 `type`、`renderer`、`parameters`、`texts`。
- renderer、参数名、参数类型和文本槽位必须通过 `SchoolVisualizationCatalog` 校验；课程不能声明任意执行代码。
- 旧 `scene` 步骤不作为兼容格式保留。

### 7.6 PDF 契约

`textbook.pdf` 固定声明：

```json
{
  "path": "assets/textbook.pdf",
  "pageCount": 202,
  "pageIndexOffset": 7
}
```

发布时必须保证：

- `path` 指向 manifest `files` 中真实存在的 PDF 文件。
- PDF 文件大小和 SHA-256 与 manifest 完全一致。
- 文件必须具有合法 PDF 头并能被 Android `PdfRenderer` 打开。
- `PdfRenderer.pageCount` 必须与 `pageCount` 完全一致。
- `pageIndexOffset` 用于印刷页码与 PDF index 的换算；制作 references 时必须按 authored course 采用的印刷页口径统一核对。

### 7.7 可选 Assessment Package

需要正式题库时，在最终课程目录同时加入：

```text
assessments.json
knowledge-points.json
assets/<question-assets>
```

规则：

- `assessments.json` 与 `knowledge-points.json` 必须同时存在或同时不存在。
- 两个文件的 `courseId` 必须与 `course.json.textbook.id` 一致。
- Assessment knowledge point 引用的 section 必须存在于 `course.json`。
- 所有 question set 都必须且只能放置到一个有效 section；不能存在未放置题组。
- 题目引用的 knowledge point 必须存在。
- 题目引用的 asset 必须声明；声明的 asset 也必须实际被题目使用。
- asset path 必须位于 `assets/` 下，使用安全正斜杠相对路径。
- 当前图片媒体类型只接受 PNG、WEBP、JPEG；声明的扩展名、MIME、宽度、高度必须与实际图片一致。
- 图片单边最大 16384 像素，总像素数不得超过 40000000。

### 7.8 完整性、更新与发布建议

- App 以 `size + SHA-256` 判断本地文件是否与远端一致；内容变化必须产生新的真实 size/hash。
- 更新时只下载变化文件；若增量传输体积达到当前全量阈值或增量文件缺少 URL，则使用完整 ZIP。
- 完整安装和增量安装都先进入 staging，所有文件、课程 JSON、题库和 PDF 验证通过后再原子替换 active；失败时保留上一份已验证课程。
- 大 PDF、图片和未来大型媒体优先使用 `bundled=false`，让正文小改动不触发大资源重复下载。
- 同一 immutable `release-id` 的对象内容禁止覆写；任何修复生成新 release，先 Testing，再提升同一 release 到 Stable。
- 不把本节模板复制成仓库中的课程示例目录；需要制作课程时在临时工作区按本节生成真实 artifact。

## 8. 数字课本编写原则

课程目标不是教材摘要、知识卡片或教材旁边的 App 解说，而是基于教材知识体系重新编写一套可以独立阅读和学习的严肃数字课本。

- 教材负责准确，App 负责理解。
- 以对应教材的知识顺序、术语、概念边界、论证路径、教学语气和详细程度为基线。
- 不大段复制教材原文；教材材料与我们自己的严谨表述有机结合。
- “先想一想”之前必须已有明确的情境、事实、例子、图示或已有知识；不能让问题本身承担背景介绍职责。
- 推荐节奏是“情境或例子 → 严谨叙述 → 观察/可视化 → 先想一想 → 概念形成或推导 → 例题 → 检查一下 → 记住 → 练习”，但服从教材真实编排，不机械套模板。
- “记住”用于完整收束定义、法则、条件、结论和必要注意事项，不写成口号式速记卡。
- 教材静态数学图、示意图、数轴、关系图和可交互模型，只要能够准确语义化，就优先使用 App Renderer 重建；保留教学意义、关键标注和信息层次，不复制出版社版式或截图。
- 教材例题和情境可以作为教学路径基线，但正文、问题和解析应重新组织，不能为了“原创”擅自改变知识难度。

## 9. 课程与 App 运行边界

- APK 提供课程运行能力；课程包只描述教学内容和受限语义调用。
- 课程包不得携带或执行 Kotlin、JavaScript、Python、Shell 或其他任意代码。
- 课程包不得通过反射、回调、类名、URL、文件路径或任意脚本获得宿主能力。
- App 对远端课程执行必要的运行时格式、安全、完整性和哈希校验；这是 App 产品能力，不是课程 CI。
- 课程更新和 App 发布完全解耦；课程 Stable manifest 更新后，已有 App 应可直接获取新课程。

## 10. Visualization 架构

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

## 11. Verification 与学科引擎

- Verification 共享输入、结果、结构化步骤、问题/警告和可视化请求，但不实现万能 Solver。
- `VerificationStep` 表示结构化规则变换，不退化成不可验证的字符串列表。
- Math、Physics、Chemistry、Biology 等拥有独立领域语义；可以复用底层数学能力，但不能混淆学科规则。
- Math Engine 不依赖 Visualization；App/UI 层负责把领域结果映射为可视化 invocation。
- 不使用 `Any`、无类型 Map、反射、脚本或任意回调作为跨学科协议。
- 本地验证没有 AI fallback；不支持时明确返回不支持。
- 当前 Math Engine 范围是初高中基础数学，不把极限、导数、积分、微分方程、Taylor 展开等高等数学混入当前本地验证。
- 英语、日语的语言验证需要形态、语法、句法、语义槽位和上下文模型；语文最后设计，开放写作和文学评价不伪装成确定性规则验证。

## 12. 代码与工具规则

- 优先删除旧实现，不长期保留无业务价值的兼容壳、alias、历史命名、退役 wrapper 和墓碑测试。
- 新架构稳定后不要为了旧调用继续增加兼容分支。
- 不引入无必要的 God class、万能 Renderer、万能 Solver 或万能协议。
- 领域内核尽量保持纯 Kotlin，避免依赖 Android、网络、数据库、AI 或 UI。
- App/UI 负责组合领域结果和基础设施，不反向向领域层注入 UI 依赖。
- 修改前检查真实引用；删除后通过 App 编译和单元测试确认依赖闭合。
- Kotlin、Java、XML 不要出现无意义换行；方法调用、参数列表和表达式在可读前提下保持紧凑。
- 不为了格式化制造大面积无关 diff。
- 禁止为了方便在仓库中新建 `tools/`。任何临时脚本默认只属于当前 Agent 执行环境。
- `scripts/` 也不是杂物目录：通常只有与 App 构建、运行、签名、更新或发布直接相关且长期需要的脚本才能提交；`scripts/course_r2_manager.py` 是课程分发基础设施的唯一长期例外。
- 涉及 Cloudflare R2 的文件/目录 CRUD、课程 release 上传、Testing/Stable 发布时必须优先复用 `scripts/course_r2_manager.py`；普通课程管理默认走 Worker backend，只有明确的底层恢复/诊断场景才使用 direct backend；除非用户明确要求替换该实现，否则不得新增功能重叠的 R2 脚本。

## 13. 文档规则

- 不恢复 `docs/`。
- 不新增架构 README、课程规范 README、Foundation 文档或平行规则文件。
- 长期项目规则只写 `AGENTS.md`。
- `.release-notes/current.md` 是当前待发布 App 的发布元数据，不是项目文档。
- 实现细节优先通过类型、测试和清晰命名表达，避免依赖大量旁路文档。