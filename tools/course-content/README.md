# Course content tools

课程内容工具属于**内容工程工具链**，不属于 App CI/CD。

项目的 GitHub Actions 只负责 App 自身的编译、测试、APK/更新包构建与发布。`courses/**` 的内容变化不应触发 App CI/CD，App CI/CD 也不负责判断某一本教材的目录、页码、知识点、答案或语言风格是否正确。

## 职责边界

App CI/CD 负责：

- Android / Kotlin 编译；
- App 与基础模块单元测试；
- visualization 隔离等 App 架构检查；
- APK / 更新包构建；
- App 发布。

课程内容工具负责：

- authored course contract / schema 校验；
- renderer / parameter 合法性校验；
- 教材结构、知识点、例题、练习和语言风格审校；
- 课程包打包；
- 在需要时由人工、Agent 或独立内容发布流程执行课程发布。

课程校验可以由课程审校 Agent、子代理、本地命令或独立内容工程流程调用，但**不得重新挂回 App GitHub Actions**。

## 当前权威契约

课程业务 JSON 结构见：

- `docs/COURSE_CONTENT_CONTRACT.md`
- `docs/VISUALIZATION_FRAMEWORK.md`

核心工具：

- `validate_authored_course.py`：校验 `textbook + knowledgePoints + chapters`、课时关系、教学步骤、练习以及严格的 visualization invocation。
- `visualization_contract.py`：课程制作侧的 renderer/schema 白名单，与 APK 中 `:visualization` 的公开调用契约保持一致。
- `validate_course_sources.py`：核对教材来源引用。
- `validate_pep_math_7_1_editorial.py`：七上数学教材专属内容审校规则，仅供课程审校流程调用，不属于 App CI/CD。
- `build_course_release.py` / `course_release_bundle.py`：从可读 `course.json` 生成不可变课程发布包。
- `publish_course_r2.py`：需要发布课程时由人工或独立内容工程流程调用。

## Authored source

每门正式课程只保留一份可读、可 diff、可审查的源文件：

```text
courses/<textbook-id>/course.json
```

`pep-math-7-1` 的权威源文件为：

```text
courses/pep-math-7-1/course.json
```

不再把 gzip/base64 分卷产物提交到仓库。压缩、打包、SHA-256 和不可变发布 ID 都属于课程发布阶段产物，不作为 authored source 维护。

## 可视化输入边界

课程包中的可视化步骤只能描述已经注册的 renderer、数学/教学语义参数和显示文字。参数类型与字段白名单以 `visualization_contract.py` 为准，其中 `mathematics.function.graph` 的 `expression` 是受限数学表达式，不是脚本，也不能访问宿主能力。

可视化基础设施本身位于独立 `:visualization` Android Library。课程工具不生成绘图代码，也不根据标题猜测绘图实现。

## 本地 / Agent 校验示例

验证正式七上数学课程：

```bash
python3 tools/course-content/validate_authored_course.py courses/pep-math-7-1/course.json
python3 tools/course-content/validate_pep_math_7_1_editorial.py courses/pep-math-7-1/course.json
```

验证全部正式课程的通用 contract：

```bash
find courses -type f -name 'course.json' -print0 | sort -z | xargs -0 python3 tools/course-content/validate_authored_course.py
```

这些命令供人工、Agent 或内容工程流程调用，不是 App CI/CD 的必经步骤。

## 已退役且不得恢复

以下内容与当前契约冲突，不应重新出现：

- `course.json.gz.b64.part*` authored source 分卷
- `tools/course_quality`
- `tools/course-content/manual`
- `generate_math_courses.py`
- `audit_math_courses.py`
- `normalize_course_contract.py`
- `pdf_asset_workflow.py`
- `postprocess_math_courses.py`
- `prepare_assessment_packages.py`
- 任何临时 `scene` → `visualization` 兼容/迁移器
- 任何将具体课程包审校或发布重新绑定到 App GitHub Actions 的 workflow

如果课程需要新增一种可视化能力，应先在 App 的 `:visualization` 基础设施中新增并注册 renderer、补齐 Kotlin/Python schema 和 App 测试，再由课程包直接引用新 key；不要在课程工具里恢复旧格式转换层。
