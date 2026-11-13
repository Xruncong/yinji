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
- **纯本地但必须能导出**：JSON 全量备份（含软删记录），免费、无需登录、不设笔数限制；用 SAF，不申请存储权限。恢复会先弹「覆盖且不可撤销」确认，清空+回灌包在 Room 事务里，中途失败自动回滚。
- 所有表预留 `updated_at` / `deleted_at`（软删）/ `device_id`，为二期同步留口子。

---

## 状态（v1 已完成）

四个功能屏、Glance 桌面小卡片、导出/恢复 UI 全部实现，`assembleDebug` 持续 BUILD SUCCESSFUL，产物 `app-debug.apk`（约 60MB，debug 签名）经 `aapt dump badging` 校验。

**已在 Android 14（API 34）模拟器逐链路调试通过**（用户 m01043 要求）：

| 链路 | 验证结果 |
| --- | --- |
| 冷启动 / 四 tab 导航 | 无崩溃，logcat 无 FATAL |
| 习惯 | 新建 → 打卡 `0/1→1/1`、取消打卡软删、连续天数重算、月历热力图补卡 → `当前 1 天 / 最长 1 天 / 近30天 3%` |
| 记账 | 分类 5 列网格与数字键盘同屏、金额 Long 存分（`¥32.80`）、月度总额/结余、环形占比图（餐饮/交通双段比例正确）、最近记录 |
| 备忘 | `#标签` 抽取成 chip、正文搜索、置顶、编辑、删除 |
| 待办 | 今天/全部/已完成分段、自然语言日期（`11/20`→`11 月 20 日 周五`）、逾期 `⚠ 已逾期` 标红、完成勾选 → 已完成「恢复」 |
| 导出/恢复 | SAF 弹出 CreateDocument/OpenDocument，导出 JSON 2251B 结构正确，恢复后 5 表计数与明细一致（事务原子性验证通过） |
| 桌面小卡片 | `dumpsys appwidget` 确认 `YinjiWidgetReceiver` provider 已注册 |

调试中发现并修复的真实 bug：`NotesScreen` 空白输入可落库（`isNotBlank()` 与 `trim()` 判断不一致）、`importJson` 无事务可致数据全丢、`Money.parse` 浮点精度、`AddTransactionDialog` 日期不可改、`todayProgress` 未按 habitId 分组、`MainActivity` 在 composition 期间写 state。

> 已知取舍：`parseNatural` 的 `M/D` 若已过去会按「最近的未来」顺延一年（沿用滴答清单语义）；补录逾期任务请用完整 `yyyy-MM-dd`。这是一期保留的默认值，非崩溃缺陷。

二期（未做）：同步/账号、电脑端、预算与分类预算、提醒通知、富文本、子任务、可量化习惯目标等，见 [`docs/PRD.md`](docs/PRD.md)。
