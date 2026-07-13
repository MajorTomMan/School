# School

一个只服务个人学习过程的 Android 原生 App。它不是网课平台，也不是简单的 PDF 阅读器；目标是把教材变成可以真正走完的学习路径：

```text
教材定位 → 动态讲解 → 独立练习 → 错因诊断 → 到期复习
```

## 当前版本：0.7.0

首期范围仍聚焦七年级数学上册第一章「有理数」。当前版本已经具备极简场景式学习界面、本地学习闭环、AI 批改、复习数据库，以及独立教材资源包导入：

- 黑、白、红、蓝、黄五色极简场景式 UI
- 动态数轴、知识推导和逐页学习过渡
- OpenAI-compatible 客户端，可连接局域网 llama.cpp
- `/v1/models` 连接测试、AI 结构化批改与本地检查兜底
- Room 保存题目、答案、正确性、反馈、错误类型和时间
- 自动维护每个知识点的复习队列与间隔
- School Material Pack v1 ZIP 导入、替换、移除和版本校验
- PDF SHA-256 完整性校验与 ZIP 路径穿越防护
- 从学习页直接打开教材对应页，并在原生 PDF 阅读器中前后翻页
- Android CI 自动测试、构建 APK，并更新 `dev-latest` 预发布版本

教材 PDF 不进入 APK，而是通过独立教材包安装到 App 私有目录。资源包规范见 [`docs/MATERIAL_PACK_V1.md`](docs/MATERIAL_PACK_V1.md)。

## 构建教材包

```bash
python scripts/build_material_pack.py \
  --pdf "/path/to/数学七年级上册.pdf" \
  --catalog app/src/main/assets/catalog/math-grade7-volume1.json \
  --output math-grade7-volume1.school.zip \
  --pack-id math-grade7-volume1 \
  --version 1.0.0 \
  --title "七年级数学上册" \
  --subject "数学" \
  --page-index-offset 0
```

生成后在 App 中进入：

```text
设置 → 教材 → 导入教材包
```

导入成功后，学习流程中的「教材定位」会出现“打开第 N 页”。

## llama.cpp 配置

在 App 的「设置」中填写：

```text
接口地址：http://电脑局域网地址:7777/v1
模型名称：与 /v1/models 返回的 id 一致
API Key：局域网服务未启用鉴权时留空
```

手机和电脑需要处于可互访的网络中。测试版为了局域网 llama.cpp 允许 HTTP 明文请求，因此不要把未鉴权接口直接暴露到公网。

## 技术栈

- Android Gradle Plugin 9.2.0
- Gradle 9.4.1
- JDK 17
- Kotlin built-in support + Compose compiler 2.3.10
- Jetpack Compose + Compose Animation + Canvas
- Room 2.8.4 + KSP 2.3.10
- Preferences DataStore
- Android Storage Access Framework、`ZipInputStream`、`PdfRenderer`
- `HttpURLConnection` + OpenAI-compatible JSON API
- JUnit 4 单元测试
- compileSdk / targetSdk 36，minSdk 26

## 本地运行

使用支持 AGP 9.2 的 Android Studio 打开仓库并运行 `app`。也可以在已安装 Gradle 9.4.1 与 Android SDK 36 的环境中执行：

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

仓库暂未提交 Gradle Wrapper 二进制文件，CI 会显式安装 Gradle 9.4.1。

## CI/CD 与自动 APK

每次 Pull Request 都会自动运行单元测试并编译 Debug APK。每次代码进入 `master` 且测试、编译成功后，工作流还会：

1. 生成 `school-debug.apk` 和 SHA-256 校验文件。
2. 上传为 GitHub Actions 构建产物。
3. 更新名为 `dev-latest` 的滚动预发布 Release。
4. Release 正文只记录本次中文修改点与修复点。

## 接下来

1. 读取 `catalog.json`，用真实教材目录替换示例课程数据。
2. 从真实教材中导出「有理数」的知识点、场景和练习。
3. 让 AI 分层提示和错因诊断直接驱动复习质量分数。
4. 加入数学公式渲染、手写和拍题。

架构约束见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)，实施顺序见 [`docs/ROADMAP.md`](docs/ROADMAP.md)。
