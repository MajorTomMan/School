## 变更点

- 新增 `core.select-one@1` 通用单项选择 Activity，课程可声明选项并通过 canonical option ID 参与 inline assessment。
- 新增 `core.order@1` 通用排序 Activity，支持逐项上下调整并以稳定的 `id1|id2|...` 结果复用现有 exactText 判定。
- 新增 `core.match@1` 通用匹配 Activity，支持一对一配对并按左侧声明顺序生成稳定的 `leftId=rightId|...` 结果。
- 三种新 Activity 已完整接入 typed decoder、ActivityRuntime、Compose UI host、运行时 capability 注册与课程兼容性校验。
- 补充 Runtime 与课程参数解析单元测试，并同步更新课程包 Activity 长期契约说明。
