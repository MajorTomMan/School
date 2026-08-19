## 变更点

- 重构 App 更新地址获取：可从远程 JSON 读取 updateUrl，未配置远程配置地址时使用默认 GitHub Release。
- 更新清单不再保存独立下载地址，APK 地址统一由 updateUrl 与文件名生成，CI 发布目标与 App 更新源解耦。
- 清理旧内置教材目录、示例课程与已停用的预制教材兼容代码。
