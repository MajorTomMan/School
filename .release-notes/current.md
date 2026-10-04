## 变更点

- 清理 Framework v1 遗留的学习数据库迁移链，旧 schema 缺失迁移路径时按当前模型执行破坏性重建。
- 将 Lesson 展示模型从 data 层迁入 UI presentation 层，继续收紧数据与界面边界。
- 设置页完成通用命名，并统一通过 LearningDataMaintenance 清理学习数据。
- 移除首页固定“初中学习”和预计学习时长文案，避免通用课程框架继续绑定单一学段假设。
