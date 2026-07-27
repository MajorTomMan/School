# School 课程包：Cloudflare Worker + R2 发布

APK 负责课程解析、校验、缓存和渲染；课程业务数据、教材 PDF、完整 ZIP 与分发清单通过 Cloudflare Worker 写入私有 R2，再由 Worker 提供公开只读下载。

```text
课程源码 / 教材 PDF
        ↓
严格 course.json + course.zip + manifest.json
        ↓ Bearer Token
course Worker
        ↓
私有 R2 Bucket
        ↓ 公开只读
School App
```

## R2 对象布局

```text
school-course/
├── releases/
│   └── <release-id>/
│       ├── manifest.json
│       ├── pep-math-7-1/
│       │   ├── course.json
│       │   ├── course.zip
│       │   └── textbook.pdf
│       └── ...
└── channels/
    ├── testing/manifest.json
    └── stable/manifest.json
```

`releases/<release-id>/` 不可变；新版本必须创建新的 release ID。testing 和 stable 只保存频道清单，因此提升与回滚只替换一个小 JSON 对象。

公开地址：

```text
https://course.flashnamesl.workers.dev/cloud/course/public/testing/manifest.json
https://course.flashnamesl.workers.dev/cloud/course/public/stable/manifest.json
https://course.flashnamesl.workers.dev/cloud/course/public/releases/<release-id>/...
```

R2 Bucket 不需要开启 `r2.dev` 或公开访问。

## 当前 APK 分发清单

根节点只允许 `textbooks`：

```json
{
  "textbooks": [
    {
      "id": "pep-math-7-1",
      "package": {
        "path": "pep-math-7-1.zip",
        "url": "https://course.flashnamesl.workers.dev/cloud/course/public/releases/20260727-abc1234/pep-math-7-1/course.zip",
        "size": 123456,
        "sha256": "..."
      },
      "files": [
        {
          "path": "course.json",
          "url": "https://course.flashnamesl.workers.dev/cloud/course/public/releases/20260727-abc1234/pep-math-7-1/course.json",
          "size": 234567,
          "sha256": "...",
          "bundled": true
        },
        {
          "path": "assets/textbook.pdf",
          "url": "https://course.flashnamesl.workers.dev/cloud/course/public/releases/20260727-abc1234/pep-math-7-1/textbook.pdf",
          "size": 12915486,
          "sha256": "...",
          "bundled": false
        }
      ]
    }
  ]
}
```

下载地址、大小和 SHA-256 只属于分发清单；`course.json` 只保存教材、章节、课程页、内容块与场景业务数据。

## 旧生成结果规范化

历史生成器仍会输出作者校对字段和旧块名。发布前运行：

```bash
python3 tools/course-content/normalize_course_contract.py \
  --source-root build/generated-course \
  --output-root build/runtime-course
```

规范化器会删除下载元数据、来源锚点和作者字段；映射旧文本、例题、列表和可视化块；把字符串布尔值与数值转为真实 JSON 类型；并为重复通用 ID 生成确定性的命名空间 ID。遇到 APK 不支持的内容时直接失败。

## 本地构建一本教材

```bash
python3 tools/course-content/build_course_release.py \
  --source /path/to/course.json \
  --pdf /path/to/textbook.pdf \
  --output build/course-release \
  --release-id 20260727-local-1 \
  --public-base-url https://course.flashnamesl.workers.dev/cloud/course/public
```

输出：

```text
build/course-release/
├── manifest.json
└── <textbook-id>/
    ├── course.json
    ├── course.zip
    └── textbook.pdf
```

## 上传到 testing

```bash
source ~/.config/course/secrets.env

python3 tools/course-content/publish_course_r2.py upload \
  --root build/course-release \
  --release-id 20260727-local-1 \
  --channel testing
```

发布器会跳过摘要一致的对象，逐个申请短时上传 URL，上传并确认大小和 SHA-256，最后调用 `/cloud/course/channel/publish` 原子发布 testing 清单。

发布器只需要 `COURSE_API_TOKEN`；不需要 `COURSE_SIGNING_SECRET` 或 R2 S3 密钥。

## 提升到 stable 与回滚

确认 testing APK 正常后：

```bash
python3 tools/course-content/publish_course_r2.py promote \
  --release-id 20260727-local-1 \
  --channel stable
```

此命令不会重新上传大文件，只会让 Worker 重新校验 release manifest 并原子写入 stable。回滚时对旧 release ID 执行同一命令。

## GitHub Actions

工作流：

```text
.github/workflows/course-r2-release.yml
```

行为：

- Pull Request：生成、校验、规范化并构建课程，但不上传；
- 合并到 `master`：创建不可变 release 并自动发布到 testing；
- 手动 `publish-testing`：重新构建并发布 testing；
- 手动 `promote-stable`：填写已验证的 release ID，只提升频道清单。

仓库需要配置：

```text
Actions Secret:
  COURSE_API_TOKEN

Actions Variable（可选）：
  COURSE_BASE_URL=https://course.flashnamesl.workers.dev
```

仓库 `gradle.properties` 已显式设置：

```text
schoolCourseManifestUrl=https://course.flashnamesl.workers.dev/cloud/course/public/stable/manifest.json
```

本地或其他发行环境仍可通过 `SCHOOL_COURSE_MANIFEST_URL` 覆盖。

## 客户端更新规则

- 本地无教材时下载完整 ZIP 与教材 PDF；
- 只有 `course.json` 变化时优先执行文件级增量更新；
- PDF 摘要未变化时复用本地文件；
- 增量失败时回退完整 ZIP；
- 所有文件先进入 staging，完成大小、SHA-256、ZIP、课程结构、场景参数和 PDF 校验后再切换 active；
- 任一步失败都保留旧课程。

## 安全边界

- R2 保持私有；
- 上传和频道发布使用 Bearer Token；
- App 只访问公开只读的 channel/release 路径；
- 公开接口不能列举 Bucket、不能读取 `incoming/`、不能写入或删除；
- release 文件不可变并长期缓存；channel manifest 使用 `no-cache`；
- stable 必须由人工确认后的 release ID 提升。
