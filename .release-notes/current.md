## 修改点

- 课程运行时改为以 `KnowledgePoint → Chapter → Section → Lesson → Step → Practice` 为核心的数据结构，教材 PDF 仅作为参考来源和页码跳转依据。
- 课程正文、例题、练习、总结和交互步骤改为原创教学内容，不再从 OCR 或教材排版生成课程页。
- 新增 `explanation`、`question`、`keyIdea`、`formula`、`example`、`scene`、`checkpoint`、`sourceLink`、`summary` 教学步骤，并将练习题结构化绑定到知识点和难度。
- 新增知识点前置图和课时前置关系校验，禁止不存在的依赖和循环依赖。
- 教材引用改为 Lesson 级 `references`，支持教材印刷页范围和 App 内直接跳转，但不参与课程正文生成。
- 新增 `validate_authored_course.py`，课程 CI 只校验新版 authored course 契约、知识图、课时、练习和教材引用范围。
- CI/CD 改为 `dev` 优先：课程契约 PR 和开发流程以 `dev` 为集成分支，`master` 留作稳定集成。

## 修复点

- 删除旧的 `CoursePage / blocks / sourceAnchors` 运行时和按页教材复刻流程，新版 APK 不提供旧课程包兼容或降级解析。
- 删除六册旧课程源、人工 page override、OCR/标题精校工具和 `course_quality` 流水线，避免新旧生产方式并存。
- 删除旧的 CloudCourse 分页教学与外挂题组拼接页面，课程入口直接读取 authored Lesson 并按教学步骤渲染。
- 删除旧 `Course R2 Release` 自动生成发布流水线；在新版课程内容完成前只运行契约验证，避免旧格式课程重新上传 R2。
- 发布说明重新按当前版本整理，不再累计已经发布过的历史修改项。
