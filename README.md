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

- **五块**：习惯打卡（首页 + 上班打卡卡）、记账、备忘、待办，底部导航 4 tab。
- **桌面小卡片**：习惯 + 待办同卡，点一下即写库完成并消失；App 内「已完成」列表可一键恢复（`done` 只标状态不删行）。刷新靠 Repository 层统一 `updateAll()`，0 点靠 `ACTION_DATE_CHANGED` + 重算今日兜底。
- **金额 `Long` 存分**，月度汇总走 Room `GROUP BY`；日期业务键用本地 `yyyy-MM-dd`，周一起始。
- **连续天数派生不落库**，按「应做日序列」相邻判断（指定周几的习惯跨周末不断链）。
- **纯本地但必须能导出**：JSON 全量备份（含软删记录），免费、无需登录、不设笔数限制；用 SAF，不申请存储权限。恢复会先弹「覆盖且不可撤销」确认，清空+回灌包在 Room 事务里，中途失败自动回滚。
- 所有表预留 `updated_at` / `deleted_at`（软删）/ `device_id`，为二期同步留口子。

---

## 视觉（v1.1「奶油薄荷 · 小芽生长」）

用户反馈初版「UI 好丑」后按 `impeccable` skill 做的纯视觉改版，**功能逻辑一律未动**。概念图见 [`docs/ui-concept.png`](docs/ui-concept.png)（豆包出图），设计令牌与提示词见 [`docs/UI-doubao-prompts.md`](docs/UI-doubao-prompts.md)。

- 奶白底 `#FBF8F1` + 薄荷绿 `#3A9A80` + 奶油黄/蜜桃粉点缀；软糖卡片（24dp 大圆角、y 偏移模糊阴影、奶油细描边）、全圆胶囊按钮/Chip。
- 用真实线性图标（`material-icons-extended`）替换文字符号导航 ◉¥✎✓ 和 emoji 图标（🔍/📌/💡）；`Sprout` 为纯 Canvas 自绘小芽，随今日进度长大。
- 进度卡由纯色方块改成浅薄荷底 + 墨绿字 + 薄荷轨道进度条；10+3 个分类色值降饱和成奶油色系；M3 `surfaceContainer*` 色 role 统一设成暖奶油，输入框不再有默认淡紫底。
- 验收：`assembleDebug` 零警告 BUILD SUCCESSFUL；Android 14 模拟器四屏截图复看；`impeccable detect --json` 返回 `[]`（零违规）。

### v1.2 功能界面细化（豆包二次出图）

用户反馈「功能界面还是太丑」后，针对概念图里没落地的装饰细节再改一轮，功能逻辑仍未动。第二张概念图见 [`docs/ui-concept-ui2.png`](docs/ui-concept-ui2.png)，提示词见 [`docs/doubao-prompt-ui2.txt`](docs/doubao-prompt-ui2.txt)。

- `SproutBuddy`：带笑脸、腮红和漂浮圆点的手绘小芽吉祥物（纯 Canvas 自绘），出现在进度卡、环形图中央和所有空态；`EmptyHint` 统一升级，内容少时下半屏不再空白。
- `CreamCard`（奶油黄强调卡）用于月度总额；`CategoryEmoji` 给分类配柔和彩色圆底，替换过淡的方块底；记一笔弹层的分类格、数字键盘改为圆角糖果感。
- 待办三分段、备忘 `#标签` 上糖果色（青绿 / 奶油黄 / 蜜桃粉，选中态压深保证白字可读）。
- 验收：零警告编译；Android 14 模拟器四屏 + 空态 + 记一笔弹层截图复看；`impeccable detect` 零违规。

### v1.3 新建/编辑弹层可爱化

用户反馈「各页新建页面太丑」后统一重做四个弹层，功能逻辑仍未动。改的是**共用底座**（一处改动，四屏与确认框一起受益），不是逐屏补妆：

- `YDialog`：纯白方块 → 顶部四色「手账胶带」（薄荷绿 / 奶油黄 / 蜜桃粉 / 浅薄荷）+ 2dp 薄荷描边 + 更大圆角与柔和阴影。
- `DialogTitle`：光秃秃一行字 → `SproutBuddy` 笑脸小芽吉祥物 + 20sp 粗标题（`buddy = false` 可关，用于标题已带 emoji 的习惯详情）。
- 新增 `CuteTextField` 统一所有输入框：暖奶油 `Field` 底、14dp 圆角、无下划线、薄荷光标、可带 leadingIcon（备忘搜索框用它）；四页原来 9 个裸 `TextField` 全部替换，记一笔的备注/日期也一并统一。
- 新增 `SectionLabel`：彩色小圆点 + 粗体分节标签，替换「图标 / 周期 / 截止」等灰小字；emoji 选择、周期按钮、日期快捷（今天/昨天）换成描边圆章与糖果胶囊。
- 验收：零警告编译；Android 14 模拟器逐个打开新建习惯 / 记一笔 / 新建待办 / 编辑备忘弹层截图复看；`impeccable detect` 零违规。

### v1.4 上班打卡提醒（新功能）

用户要求「增加上班打卡提醒」，先用豆包出四宫格概念图确认方向再实现。概念图见 [`docs/ui-concept-clockin.png`](docs/ui-concept-clockin.png)，提示词见 [`docs/doubao-prompt-clockin.txt`](docs/doubao-prompt-clockin.txt)。四格全部做完：

- **首页打卡卡**（今日页顶部插卡，不破四 tab 底栏）：奶油黄卡 + 圆形进度环小芽，点小芽/「打上班卡」即记到岗时间，另有「打下班卡」。底部进「打卡记录」与「提醒设置」。
- **提醒设置**：开关 + 上班 09:00 / 下班 18:00 时间选择 + 周一~周五糖果圆 + 提前 5 分钟（准点/5/10/15 分可选），存 SharedPreferences；保存即重排闹钟。
- **到点通知**：`AlarmManager` 精确定时（无精确闹钟权限自动退回非精确）+ 本地通知，带「已打卡」（`goAsync()` 协程写库、撤通知）/「稍后提醒」（10 分钟后再响）两个按钮；开机/改时间/跨日/升级自动重排；Android 13+ 运行时申请 `POST_NOTIFICATIONS`。
- **打卡记录**：本月出勤 / 连续打卡 / 迟到三张小卡 + 周一起始月历热力图；连续天数「休息日不打断」，周末打卡也算数。
- **数据层**：新增 `clock_in` 表（`(date, type)` 联合主键，一天最多上班/下班两条），Room `version=1 → 2` 用**显式 `MIGRATION_1_2`** 保留真机旧数据；导出/恢复 JSON 增加第 6 张表（`BackupCodec.VERSION` 仍 1，旧备份缺键自动空数组向后兼容）；纯本地、不联网、无会员墙、不引新依赖。
- 验收：零警告编译；Android 14 模拟器逐链路实测——打卡卡渲染、两条打卡写库（`2026-11-15|on|03:47` / `off`）、统计热力图、提醒设置、`dumpsys alarm` 确认两个闹钟已排、以 App 自身 UID 触发到点通知（`NotificationRecord id=1001` / `channel=clock_in_reminder` / `actions=2`）、点「已打卡」按钮写库成功且通知撤除；`impeccable detect` 零违规。

### v1.5 桌面小组件视觉统一（原型 + 真机卡片）

用户反馈「原型里的小组件和真机/概念图不一样」，要求原型与 App 都对齐豆包概念图。概念图见 [`docs/ui-concept-widget.png`](docs/ui-concept-widget.png)，提示词见 [`docs/doubao-prompt-widget.txt`](docs/doubao-prompt-widget.txt)。

- **真机 Glance 卡片重写**：奶白软糖卡（外层奶油描边 + 内层奶白双层模拟圆角/细描边）、顶部四色手账胶带、标题行笑脸小芽（`res/drawable/widget_sprout.xml` 自绘 vector）、蜜桃粉/奶油黄圆点分节、薄荷空心圆勾选框（`widget_circle.xml`）、「勾掉即消失」仍靠 `Repository.widgetSnapshot()` 只返回未完成项过滤，点击走 `ActionCallback` 写库 + `update` 刷新。Glance 1.1.1 无 border/shadow/Canvas，故用 vector drawable + 双层 background 实现。
- **原型 `prototype/index.html` 同步**：CSS、内联 `widgetHTML()`、静态首屏兜底三处一起换成同款（四色胶带 + 内联笑脸小芽 SVG + 圆点分节 + 空心圈），`yinji-prototype.html` 保持 SHA256 一致；`node --check` 通过、Edge 渲染实读正常。
- **验收**：零警告编译；Android 14 模拟器用 `input draganddrop` 真正把 4×2 卡片放置到 Launcher3 桌面（`dumpsys appwidget` 出现 `[1] id=6 provider=...YinjiWidgetReceiver`），截图实读样式与概念图一致；点空心圆圈 → sqlite 新增 `1|2026-11-15|` 完成记录且卡片上该习惯与「今日习惯」分节一起消失（查询过滤生效）；`impeccable detect` 零违规。

### v1.6 豆包动画小猫做进 App（会动的角色）

用户要求「动画也让它画，用它做的做进 app 里」——不再用代码自绘小芽，改用**豆包生成的简笔画小猫真帧**做吉祥物，在 App 内逐帧播放。角色设定图 [`docs/ui-concept-cat.png`](docs/ui-concept-cat.png)、动画帧序列图 [`docs/ui-concept-cat-anim.png`](docs/ui-concept-cat-anim.png)（4×2=8 格同一只猫的呼吸/摆尾/眨眼相位），提示词见 [`docs/doubao-prompt-cat-anim.txt`](docs/doubao-prompt-cat-anim.txt)。

- **切帧**：`_debug/slice_cat.py`（Pillow）按 4×2 等分切 8 格、去水印、近白像素转透明（阈值 243 保住蜜桃粉腮红）、居中到 220×240 透明画布，双写 `docs/cat_frames/` 与 `app/src/main/res/drawable/cat_01..08.png`；另生成紧凑裁切的 `widget_cat.png` 供小组件用。
- **App 内会动**：`SproutBuddy` 签名不变（`modifier` + `color`），实现从「Canvas 自绘小芽」换成 `LaunchedEffect` + 130ms/帧循环切换 8 张 `painterResource`，今日进度卡、记账环形图中心、空态插画、打卡卡、弹层标题等所有调用点一行未改即全部换成会动的豆包猫。
- **小组件同步**：`YinjiWidget.kt` 标题行与空态的图标从 `widget_sprout` 换成 `widget_cat`（Glance 是静态 RemoteViews，用单帧静态猫；逐帧动画只在 Compose 内）。
- **可分享资产**：8 帧合成 [`docs/cat-live.gif`](docs/cat-live.gif) / [`docs/cat-live.mp4`](docs/cat-live.mp4)（8fps 循环）。
- **验收**：零警告编译；模拟器装后冷启动无崩溃，控件树确认两处「小猫同学」渲染；两帧间隔 260ms 像素差 2860 像素证明动画真在跑；一次真实写库触发 `Repository.refreshWidget()` 的 `updateAll` 后桌面卡片正确重建并显示豆包猫（四色胶带 + 右上角线稿小猫）；`impeccable detect` 零违规。

### v1.7 App 图标 + 各页汇总信息卡（豆包重新设计）

用户要求「app 的图标、每个页面的汇总信息 ui 都重新用豆包设计一下，再更新上去」。概念图见 [`docs/ui-concept-icon-summary.png`](docs/ui-concept-icon-summary.png)（2×3 六格：今日进度 / 记账本月 / 备忘 / 待办 / 打卡统计 / App 图标），提示词见 [`docs/doubao-prompt-icon-summary.txt`](docs/doubao-prompt-icon-summary.txt)。统一签名 = **顶部四色手账胶带 + 大圆角软糖卡 + 深墨绿大数字/标签 + 小猫吉祥物 + 糖果胶囊**。功能逻辑一行未动。

- **App 图标**：概念图第 6 格裁图带「豆包 AI」水印且会被系统裁切 → 不直接用；改用已切好的透明豆包猫帧 `res/drawable/cat_01.png` 做 adaptive icon 前景，Pillow 脚本 `_debug/make_launcher.py` 复刻「奶白底 + 圆角白卡 + 居中简笔小猫 + 左上斜贴四色胶带」。5 个密度 `mipmap-{mdpi..xxxhdpi}` 全出 `ic_launcher.png` / `ic_launcher_round.png` / `ic_launcher_fg.png`，`mipmap-anydpi-v26` 配 `@color/ic_launcher_background`（#FBF8F1）+ foreground + monochrome，`AndroidManifest.xml` 的 application 加 `android:icon` / `android:roundIcon`。不再用系统机器人图标。
- **共用 `SummaryCard`**（一处改动，五处调用）：四色手账胶带顶条 + 软糖卡（可传底色/描边）+ 内容槽，与 `YDialog` 同一套胶带。
  - 今日进度卡：外层 `SummaryCard` + 内层浅薄荷块，新增 `pct%` 糖果胶囊，右侧小猫随进度长大。
  - 记账：月度总额换奶油黄 `SummaryCard`（三列金额 + 竖分隔原样保留），分类占比卡也带上胶带，环形图中央小猫不变。
  - 备忘：新增汇总卡（44sp 大数字「N 条备忘」+ 热度最高 4 个 `#标签` 糖果 Pill + 小猫），复用已有 `tagsOf()`。
  - 待办：三分段 Chip 包进 `SummaryCard`（「待办一览」+ 小猫），三个 Chip `weight(1f)` 等宽。
  - 打卡统计：三张 `StatMini` 数字由 17sp → 22sp，与概念图大数字一致。
- **验收**：`assembleDebug` 零警告；Android 14 模拟器装后冷启动无 FATAL，四屏汇总卡 + 打卡统计弹层逐张截图实读（胶带/大数字/糖果胶囊/小猫齐全、无挤压遮挡），应用抽屉里确认新图标生效；`impeccable detect --json` 返回 `[]`（零违规）。

### 会动的小猫（独立互动演示）

[`prototype/sprout-live.html`](prototype/sprout-live.html) 是单文件、双击即玩的简笔画小猫互动演示，与 App 工程相互独立（不改动任何 Kotlin 代码）；配套导出的 [`docs/sprout-live.gif`](docs/sprout-live.gif) 与 [`docs/sprout-live.mp4`](docs/sprout-live.mp4) 是可直接分享的动图/视频资产。

按 [`docs/ui-concept.png`](docs/ui-concept.png) 视觉权威重做过界面壳：奶白底、软糖白卡（大圆角 + 柔阴影 + 奶油细描边）、顶部四色手账胶带、品牌栏、糖果色圆点、线性 SVG 图标按钮（不再用 emoji 当图标）；角色是纯线条画的坐姿小猫（圆脸、双耳、胡须、尾巴、腮红、ω 嘴），全部交互与确定性截帧接口保留。

- **常驻动画**：呼吸缩放、上下浮动、尾巴与耳朵摆动、周期性眨眼、嘴形随心情变化、腮红脉冲、光晕呼吸、确定性漂浮粒子。
- **互动**：点击喂小鱼干（三次长大、再点归零并撒花）、长按摸头（眯眼爱心）、拖拽甩一甩（眩晕 + 弹簧回弹）、双击哄睡/唤醒、戳脸/耳朵/肚皮各有不同文案与特效、鼠标移动时眼睛会跟随、6 种按钮入口（喂小鱼/摸头/睡觉/重来/4 种心情/可选音效）、键盘空格/Z/1-4/R/M 快捷键。
- **验收**：playwright + Edge 真实指针事件逐项验证（含单击/双击/长按/拖拽），`JS_ERRORS 0`；`window.setFrame(i,total)` / `setProgress(p)` 确定性截帧接口保留，72 帧经 ffmpeg 合成 GIF/MP4，抽帧实读正常；`impeccable detect` 的对比度问题已全修，剩余 2 项（奶白底 / 简笔画）为设计图与需求钉死的 brief wins。互动态截图见 [`prototype/sprout-interactive.png`](prototype/sprout-interactive.png) / [`prototype/sprout-sleep.png`](prototype/sprout-sleep.png) / [`prototype/sprout-bloom.png`](prototype/sprout-bloom.png)。

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
| 桌面小卡片 | v1.5 起已在模拟器真实启动器放置 4×2 卡片并验证勾选写库+消失（`dumpsys appwidget` 有实例 `id=6`）；真机待秋殇手动放置 |

调试中发现并修复的真实 bug：`NotesScreen` 空白输入可落库（`isNotBlank()` 与 `trim()` 判断不一致）、`importJson` 无事务可致数据全丢、`Money.parse` 浮点精度、`AddTransactionDialog` 日期不可改、`todayProgress` 未按 habitId 分组、`MainActivity` 在 composition 期间写 state。

> 已知取舍：`parseNatural` 的 `M/D` 若已过去会按「最近的未来」顺延一年（沿用滴答清单语义）；补录逾期任务请用完整 `yyyy-MM-dd`。这是一期保留的默认值，非崩溃缺陷。

二期（未做）：同步/账号、电脑端、预算与分类预算、提醒通知、富文本、子任务、可量化习惯目标等，见 [`docs/PRD.md`](docs/PRD.md)。
