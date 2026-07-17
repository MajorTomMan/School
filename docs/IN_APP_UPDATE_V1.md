# School 应用内在线升级 V1

## 更新链路

```text
master 构建成功
→ GitHub Actions 生成固定签名 APK
→ 自动生成 update-manifest.json
→ 使用 RSA-SHA256 签名更新清单
→ 发布到 dev-latest Release
→ App 启动或后台周期检查
→ 展示修改点和修复点
→ 下载完整 APK
→ 校验文件、包名、版本和证书
→ Android 系统安装器确认覆盖升级
```

普通第三方应用不能静默安装 APK。V1 在校验通过后调用 Android 系统安装器，由用户确认升级。

## Release 产物

`dev-latest` 包含：

```text
school-debug.apk
school-debug.apk.sha256
school-debug.apk.cert-sha256
update-manifest.json
update-manifest.sig
```

更新清单包含：

- 单调递增的 `versionCode`
- 可读 `versionName`
- 是否强制更新
- 最低支持版本
- 修改点
- 修复点
- APK 下载地址
- 文件大小
- APK SHA-256
- APK 签名证书 SHA-256

修改点和修复点由 CI 从 `.release-notes/current.md` 自动提取。

## 版本规则

CI 使用：

```text
versionCode = 100000 + GITHUB_RUN_NUMBER
versionName = 基础版本 + dev.流水线编号
```

Android 只通过 `versionCode` 判断新旧，不使用字符串比较版本。

本地没有 CI 环境变量时，继续使用 `app/build.gradle.kts` 中的基础版本。

## 检查时机

- App 进入前台且距离上次检查超过六小时。
- WorkManager 每二十四小时执行一次持久化检查。
- 设置页可以手动检查。
- 默认自动检查开启。
- 默认仅在非计费网络下载。

普通更新允许：

- 稍后提醒：二十四小时内不再自动弹窗。
- 忽略此版本：此 `versionCode` 不再自动弹窗。

当更新清单设置 `mandatory=true`，或当前版本低于 `minimumSupportedVersionCode` 时，不提供忽略入口。

## 安全检查

更新清单先通过内置 RSA 公钥验证 `update-manifest.sig`。签名失败时，不读取其中的 APK 地址。

APK 下载完成后依次验证：

1. 文件长度与清单一致。
2. 文件 SHA-256 与清单一致。
3. 包名为 `com.majortomman.school`。
4. APK `versionCode` 与清单一致。
5. APK 签名证书与固定开发证书一致。

任一步失败都会删除临时文件，不进入系统安装流程。

## 更新清单签名密钥

开发通道默认使用仓库中的公开开发密钥：

```text
signing/school-update-development-private.pem.b64
signing/school-update-development-public.pem
signing/school-update-development-public.der.b64
```

它只用于公开开发版，不能作为生产安全边界。

CI 支持 GitHub Secret：

```text
SCHOOL_UPDATE_PRIVATE_KEY_B64
```

配置该 Secret 后优先使用 Secret 中的私钥，并强制检查它是否与 App 内置公钥匹配。

正式发布版本必须使用私有更新签名密钥，并同时切换 App 内置公钥。生产私钥不得提交到仓库。

## 安装权限

App 声明：

```xml
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

首次安装更新时，如果 School 尚未获得“安装未知应用”权限，会跳转到系统授权页。授权后返回 App，再次点击“立即安装”。

通过 `FileProvider` 只暴露：

```text
files/updates/
```

系统安装器只获得目标 APK 的临时只读权限。

## UI 状态

全局升级弹窗支持：

```text
Checking
UpToDate
Available
Downloading
Ready
Error
```

设置页显示：

- 当前版本号和 `versionCode`
- 更新通道
- 自动检查开关
- 仅 Wi-Fi 下载开关
- 上次检查时间
- 当前更新状态
- 手动检查入口

## 测试方式

由于 PR 构建不会更新 `dev-latest`，完整在线升级链路需要：

1. 先将本功能和固定签名合入 `master`。
2. 等待主分支生成第一个带更新清单的 `dev-latest`。
3. 安装该版本。
4. 再合入一次提高流水线编号的修改。
5. 打开旧版本，等待启动检查或在设置页点击“检查更新”。
6. 验证变更说明、下载、校验、未知来源授权和覆盖安装。

PR APK 可以验证界面、设置项、错误处理和构建产物，但不能把同一个 PR 自己发布成远端正式更新源。
