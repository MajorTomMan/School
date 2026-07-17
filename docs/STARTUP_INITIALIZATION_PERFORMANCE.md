# School 启动初始化性能

## 问题

旧实现通过三个 `ContentProvider` 在 `MainActivity` 创建之前同步执行初始化：

- 数学预制教材安装；
- 其余三十四本预制教材目录安装；
- 掌握度趋势数据库监听。

其中全学科目录需要读取七段 Base64 资产、解码、GZIP 解压并解析一千零二十七个课程节点。即使教材已经安装，新的应用进程仍会重新解析目录以判断版本。首次安装还会同步生成教材、课程和分析 JSON。因为 `ContentProvider.onCreate()` 运行在启动关键路径上，这些操作会直接推迟 Activity 和 Compose 首帧。

## 新流程

```text
进程启动
→ MainActivity 安装 Compose 内容
→ 下一帧记录启动耗时
→ 等待 750 ms，让首屏稳定
→ IO 线程检查小型版本标记
→ 标记一致：立即结束
→ 标记缺失或过期：后台安装预制教材
→ 安装成功后刷新 MaterialPackRepository
→ 再延后启动掌握度趋势监听
```

Manifest 不再注册 School 自己的启动初始化 Provider。预制目录和趋势监听由 `StartupInitializationCoordinator` 管理。

## 快速路径

初始化成功后写入：

```text
SharedPreferences: school-startup-initialization
key: prebuilt-version
```

后续冷启动只读取一个小型字符串，不再解压和解析全部教材目录。预制资产格式或生成逻辑变化时必须更新 `CURRENT_PREBUILT_VERSION`，从而触发一次后台重建。

## 失败恢复

只有数学目录和全学科目录都安装成功后才写入版本标记。进程被杀、磁盘写入失败或解析异常时不会推进标记，下次打开 App 会重新尝试。错误写入 `SchoolStartup` 日志，不阻塞首屏。

## 状态刷新

后台安装完成后调用 `MaterialPackRepository.refreshCurrent()`。其 `StateFlow` 更新会触发 Compose 刷新，并由 `SchoolApp` 继续同步课程树，因此首次安装无需重启 App。

## 测量

```bash
adb logcat -c
adb shell am force-stop com.majortomman.school
adb shell monkey -p com.majortomman.school 1
adb logcat -s SchoolStartup
```

日志包括：

- Activity 与进程到首帧回调的耗时；
- 是否走预制教材快速路径；
- 后台目录安装耗时；
- 掌握度分析监听启动耗时。

系统级启动测量也可使用：

```bash
adb shell am force-stop com.majortomman.school
adb shell am start -W com.majortomman.school/.MainActivity
```

重点观察 `ThisTime`、`TotalTime` 和多次冷启动的稳定范围。
