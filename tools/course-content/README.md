# Course content tools

课程内容工具只负责**当前 authored course contract** 的校验、打包和发布，不承担 OCR 页面生成、旧 scene 适配、旧 assessment 手工拼装或可视化实现转换。

## 当前权威流程

课程业务 JSON 结构见：

- `docs/COURSE_CONTENT_CONTRACT.md`
- `docs/VISUALIZATION_FRAMEWORK.md`

核心工具：

- `validate_authored_course.py`：校验 `textbook + knowledgePoints + chapters`、课时关系、教学步骤、练习以及严格的 visualization invocation。
- `visualization_contract.py`：课程制作侧的 renderer/schema 白名单，与 APK 中 `:visualization` 的公开调用契约保持一致。
- `validate_course_sources.py`：核对教材来源引用。
- `build_course_release.py` / `course_release_bundle.py`：从可读 `course.json` 生成不可变课程发布包。
- `publish_course_r2.py`：上传 Cloudflare R2 并发布 testing/stable channel。

## Authored source

每门正式课程只保留一份可读、可 diff、可审查的源文件：

```text
courses/<textbook-id>/course.json
```

`pep-math-7-1` 的权威源文件为：

```text
courses/pep-math-7-1/course.json
```

不再把 gzip/base64 分卷产物提交到仓库。压缩、打包、SHA-256 和不可变发布 ID 都属于发布阶段产物，由 CI 生成。

CI 会直接拒绝任何 `course.json.gz.b64.part*` 重新进入 `courses/`。

## 可视化输入边界

课程包中的可视化步骤只能描述已经注册的 renderer、数学/教学语义参数和显示文字。参数类型与字段白名单以 `visualization_contract.py` 为准，其中 `mathematics.function.graph` 的 `expression` 是受限数学表达式，不是脚本，也不能访问宿主能力。

可视化基础设施本身位于独立 `:visualization` Android Library。课程工具不生成绘图代码，也不根据标题猜测绘图实现。

## 校验

验证正式七上数学课程：

```bash
python3 tools/course-content/validate_authored_course.py courses/pep-math-7-1/course.json
```

验证全部正式课程：

```bash
find courses -type f -name 'course.json' -print0 | sort -z | xargs -0 python3 tools/course-content/validate_authored_course.py
```

## 已退役且不得恢复

以下内容与当前契约冲突，CI 或代码审查应拒绝它们重新出现：

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

如果课程需要新增一种可视化能力，应先在 `:visualization` 中新增并注册 renderer、补齐 Kotlin/Python schema 和测试，再由课程包直接引用新 key；不要在课程工具里恢复旧格式转换层。
