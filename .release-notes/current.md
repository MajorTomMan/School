## 修改点

- 新增 Cloudflare Worker + 私有 R2 课程发布链路，课程文件使用不可变 release 目录，并通过 testing 与 stable 双频道清单发布。
- APK 默认接入 stable 课程清单地址，课程包更新与 APK 发布继续保持独立。
- 新增严格课程契约规范化、R2 上传、testing 发布、stable 提升和回滚工具，并恢复独立课程 CI 工作流。

## 修复点

- 修复旧课程生成结果包含已废弃协议字段、旧内容块名称和字符串类型场景参数，无法被新版 APK 严格解析的问题。
- 清理仍指向 GitHub Release 与 Google Drive 分发流程的过期课程发布说明，统一改为 Cloudflare R2 私有存储和 Worker 只读分发。
