# School

一个只服务个人学习过程的 Android 原生 App。它不是网课平台，也不是简单的 PDF 阅读器；目标是把教材变成可以真正走完的学习路径：

```text
教材定位 → 直觉讲解 → 分层提示 → 独立练习 → 错因诊断 → 到期复习
```

## 当前原型

首期范围刻意压小到：

- Android 原生，Kotlin + Jetpack Compose
- 七年级数学上册
- 第一章「有理数」
- 今日学习、课程路径、学习页、基础练习、复习、AI 设置
- 使用静态示例数据，不依赖账号或服务器即可打开

当前 UI 已经可以完整走通：今日学习 → 打开“数轴” → 查看讲解/教材定位/例题 → 独立作答 → 分层提示与本地反馈。

## 技术栈

- Android Gradle Plugin 9.2.0
- Gradle 9.4.1
- JDK 17
- Kotlin built-in support + Compose compiler 2.3.10
- Compose BOM 2026.06.00
- Activity Compose 1.13.0
- compileSdk / targetSdk 36，minSdk 26

## 本地运行

使用支持 AGP 9.2 的 Android Studio 打开仓库并运行 `app`。也可以在已安装 Gradle 9.4.1 与 Android SDK 36 的环境中执行：

```bash
gradle :app:assembleDebug
```

仓库暂未提交 Gradle Wrapper 二进制文件，CI 会显式安装 Gradle 9.4.1。

## 接下来

1. 加入 Room，持久化学习进度、作答和错题。
2. 实现 OpenAI-compatible 客户端，首先连接局域网 llama.cpp。
3. 定义教材资源包导入协议。
4. 从真实教材中导出“有理数”章节页面、知识点与练习。
5. 加入数学公式渲染、手写/拍题和真正的 AI 错因诊断。

架构约束见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)。
