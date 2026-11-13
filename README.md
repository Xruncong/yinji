# 印记（Yinji）

一个**纯本地、不联网、无账号、无广告**的 Android 日常记录 App：习惯打卡、记账、备忘录、待办四合一，外加一张可直接勾选的桌面小卡片。

- 📄 产品文档：[`docs/PRD.md`](docs/PRD.md)
- 🖱️ 可交互原型：用浏览器打开 [`prototype/index.html`](prototype/index.html)（**单文件、自包含、首屏静态兜底**，无需服务器、无需联网；若中文文件名/预览器不执行脚本导致空白，改开 ASCII 名的 [`prototype/yinji-prototype.html`](prototype/yinji-prototype.html)，内容完全相同）。静态截图见 [`prototype/shots/`](prototype/shots/)
- 📱 平台：原生 Android（Kotlin + Jetpack Compose + Room + Jetpack Glance），minSdk 31 / compileSdk 34

> 原型是浏览器模拟，不是真机截图；App 工程在 `app/` 下。

---

## 快速开始（本机命令行构建，不需要 Android Studio）

本机环境已装在 `D:\soft` 下：

| 组件 | 路径 |
| --- | --- |
| JDK 17 | `D:\soft\jdk-17.0.19+10` |
| Android SDK | `D:\soft\android-sdk`（platforms: android-34 / android-36） |
| Gradle 8.9 | `D:\soft\gradle-8.9\bin\gradle.bat` |

```powershell
$env:JAVA_HOME = "D:\soft\jdk-17.0.19+10"
cd E:\Projects\yinji
& "D:\soft\gradle-8.9\bin\gradle.bat" assembleDebug --no-daemon
```

产物：`app/build/outputs/apk/debug/app-debug.apk`（约 60 MB，debug 签名，可直接 `adb install`）。

`local.properties` 里写了 `sdk.dir=D:\\soft\\android-sdk`，该文件不入库；换机器时按本机 SDK 路径改一下即可。仓库里的 `gradlew` 使用官方 `https://services.gradle.org/distributions/gradle-8.9-bin.zip`，在有外网的机器上可直接 `./gradlew assembleDebug`。

---

## 版本约束（踩过的坑，别改）

| 组件 | 版本 | 原因 |
| --- | --- | --- |
| AGP | 8.5.2 | 本机已缓存 |
| compileSdk / targetSdk | **34** | 官方要求 API 36 最低 AGP 8.9.1，本机 8.5.2 不够 → 降到 34 彻底避开 |
| Kotlin | 2.0.20 | |
| Compose Compiler | `org.jetbrains.kotlin.plugin.compose:2.0.20` | Kotlin 2.x 起与 Kotlin **lockstep**，不要再用 `composeOptions.kotlinCompilerExtensionVersion` |
| KSP | 2.0.20-1.0.25 | Room 注解处理 |
| **Glance** | **1.1.1** | 1.3.x 需 compileSdk 37 / AGP 9.2.0；1.2.0 未经实测。1.1.1 有官方 codelab 与生产项目双重实证，且已下载 AAR 用 `javap` 核对过全部关键签名 |
| Room | 2.6.1 | |
| Compose BOM | 2024.10.01 | |

---

## 目录

```
app/src/main/java/com/qiushang/yinji/   Kotlin 源码
app/src/main/res/                      资源
docs/PRD.md                            产品需求文档（含调研借鉴、数据模型、验收清单）
prototype/index.html + yinji-prototype.html  自包含可交互原型（同一份，静态首屏兜底）
prototype/app.js                            原型脚本源码（已内联进上面的 HTML）
prototype/shots/                            四张页面截图
schemas/                               Room 导出的 schema（KSP 生成）
```

---

## 设计要点（详见 PRD）

- **四块全上但每块做薄**：习惯打卡（首页）、记账、备忘、待办，底部导航 4 tab。
- **桌面小卡片**：习惯 + 待办同卡，点一下即写库完成并消失；App 内「已完成」列表可一键恢复（`done` 只标状态不删行）。刷新靠 Repository 层统一 `updateAll()`，0 点靠 `ACTION_DATE_CHANGED` + 重算今日兜底。
- **金额 `Long` 存分**，月度汇总走 Room `GROUP BY`；日期业务键用本地 `yyyy-MM-dd`，周一起始。
- **连续天数派生不落库**，按「应做日序列」相邻判断（指定周几的习惯跨周末不断链）。
- **纯本地但必须能导出**：CSV 明细 + JSON 全量备份，免费、无需登录、不设笔数限制；用 SAF，不申请存储权限。
- 所有表预留 `updated_at` / `deleted_at`（软删）/ `device_id`，为二期同步留口子。

---

## 状态

工程骨架已通过 `assembleDebug`（BUILD SUCCESSFUL）；四个功能屏、Glance 卡片、导出恢复仍在实现中。进度见 [`docs/PRD.md`](docs/PRD.md) §11 验收清单。
