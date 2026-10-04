## 0.28.0 Development

Framework v1 开发预览版。本版本包含破坏性架构更新，旧版地学习数据与内部模型不保证兼容。

## 变更点

- 完成 Framework v1 主干改造：课程运行时统一以 CourseDocument、LessonRuntime 与 LessonSessionController 为核心。
- 重建学习数据链：统一 CourseProgress、LearningEvidence、KnowledgePointState 与 Mastery，收敛到新的 SchoolLearningDatabase。
- 统一 LearningContent 解析与渲染，删除旧 CloudCourseBlockRenderer、AssessmentLearningContentRenderer 等重复渲染链。
- 建立 Activity Runtime / Capability 机制，并接入数轴等交互活动与 inline assessment。
- 清理旧题库、旧数学模板、旧学习数据库、旧复习调度及已废弃的兼容模型与页面。
- 重构课程库、课程完整性与运行时兼容校验，课程仅保留可实际运行的对象。
- 调整 Today / Practice / My 等主要界面及主题系统，为后续课程内容开发提供稳定 UI 基线。
- 保留 development 更新链：固定开发签名、签名更新清单与 dev-latest 滚动发布。
