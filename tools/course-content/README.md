# Course content tools

课程内容工具只负责**当前 authored course contract** 的校验、打包和发布，不再承担 OCR 页面生成、旧 scene 适配、旧 assessment 手工拼装或可视化实现转换。

## 当前权威流程

课程业务 JSON 结构见：

- `docs/COURSE_CONTENT_CONTRACT.md`
- `docs/VISUALIZATION_FRAMEWORK.md`

核心工具：

- `validate_authored_course.py`：校验 `textbook + knowledgePoints + chapters`、课时关系、教学步骤、练习以及严格的 visualization invocation。
- `visualization_contract.py`：课程制作侧的 renderer/schema 白名单，与 APK 中 `:visualization` 的公开调用契约保持一致。
- `validate_course_sources.py`：核对教材来源引用。
- `build_course_release.py` / `course_release_bundle.py`：生成不可变课程发布包。
- `publish_course_r2.py`：上传 Cloudflare R2 并发布 testing/stable channel。

## 可视化输入边界

课程包中的可视化步骤只能是：

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
    "note": "两个点到 0 的距离相等。"
  }
}
```

约束：

- `parameters` 只接受 number、boolean、number[]；
- `texts` 只接受字符串；
- 每个 renderer 都有精确字段白名单；
- 未注册 renderer、未知字段、缺失 required 字段、错误类型直接拒绝；
- 不接受 URL、文件路径、JSON Object、任意 Map、脚本或回调；
- 不再接受 `scene/template/data`。

可视化基础设施本身位于独立 `:visualization` Android Library。课程工具不生成绘图代码，也不根据标题猜测绘图实现。

## 校验

验证一个普通课程 JSON：

```bash
python3 tools/course-content/validate_authored_course.py courses/examples/pep-math-7-1-course.json
```

验证仓库中分卷保存的正式课程时，由 `.github/workflows/course.yml` 负责恢复 gzip/base64 分卷、校验固定大小和 SHA-256，再调用同一个 validator。

正式 `pep-math-7-1` 当前恢复结果：

```text
course.json size   270303 bytes
course.json sha256 30eb89c3f6c292f461df74541f901e87ec2391bdbc415d6ff9559de6057eddc6
parts              8
```

## 已退役且不得恢复

以下管线与当前契约冲突，CI 会拒绝它们重新出现：

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
