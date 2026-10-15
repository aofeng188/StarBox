# 星匣

一个「深空液态玻璃」风格的 iOS 日常百宝箱：运势、星座、求签、塔罗，以及各种帮你做决定的随机小工具。全部离线运行，数据只保存在本机。

- 系统要求：iOS 26 及以上（使用原生 Liquid Glass）
- 已适配 iPhone Duo：外屏、内屏横持 / 竖持、半折叠都有对应排版（需用 Xcode 27.1 及以上构建）
- 开发环境：Xcode 26/27，SwiftUI + SwiftData + RealityKit + WidgetKit + App Intents
- 无第三方依赖，不需要额外下载 Metal 工具链

## 装到自己的 iPhone（免费 Apple ID）

1. 用 Xcode 打开 `XingXia.xcodeproj`。
2. 在 Xcode ▸ Settings ▸ Accounts 里登录你的 Apple ID。
3. 左侧选中项目 ▸ 分别选中 **XingXia** 和 **XingXiaWidgetsExtension** 两个 Target ▸ Signing & Capabilities ▸ Team 选择你的「Personal Team」。
4. 如果提示 Bundle Identifier 已被占用，把两个 Target 的 `com.tianaofeng.xingxia` 前缀改成你自己的（小组件保持 `<前缀>.widgets` 的形式），同时把两个 `.entitlements` 文件和 `Shared/Core/AppGroup.swift` 里的 App Group 名改成一致的 `group.<前缀>`。
5. 手机用数据线连上 Mac，顶部选择你的 iPhone，点运行。第一次运行需要在手机「设置 ▸ 通用 ▸ VPN 与设备管理」里信任开发者，并在「设置 ▸ 隐私与安全性」里打开「开发者模式」。

> 免费签名的 App 每 7 天需要用 Xcode 重新安装一次。
>
> 如果免费账号无法添加 App Groups（签名报错），在两个 Target 的 Signing & Capabilities 里删掉 App Groups 即可。App 会自动退回本地存储，功能都能正常使用；只是小组件读不到你的档案，会显示通用运势，倒数日小组件会提示打开 App。

## 不用 Xcode 安装（未签名 IPA）

[Releases](../../releases) 里提供未签名的 `XingXia-1.0.3-unsigned.ipa`。它**不能**直接点开安装，需要用侧载工具配合你自己的 Apple ID 重新签名：

1. 在电脑上安装 Sideloadly、AltStore 或 SideStore 中的任意一个。
2. 用工具打开下载的 IPA，登录 Apple ID，选择你的 iPhone 后安装。
3. 首次打开前，在手机「设置 ▸ 通用 ▸ VPN 与设备管理」里信任你的 Apple ID，并打开「开发者模式」。

> 免费 Apple ID 重签的 App 同样 7 天后过期，需要重新签一次。
>
> 如果工具报扩展或 App Group 相关的错误，选择「移除 App 扩展」后再装：App 本体功能不受影响，只是没有小组件和控制中心按钮。

## Android 版

`android/` 目录是原生 Kotlin + Jetpack Compose 的 Android 版（Android 8.0 及以上），与 iOS 版共用同一套文案 JSON，运势算法和随机种子与 iOS 完全一致：同一个人同一天在两台设备上看到的运势相同。

**直接安装**：在 [Releases](../../releases) 里下载最新的 `XingXia-android-<版本>.apk`，传到手机安装（需要在系统设置里允许「安装未知来源应用」）。安装包用 debug 证书签名，只适合自己侧载。每次推送到 main 的构建也可以在 Actions ▸ Android 的 `XingXia-android` 构建产物里下载。

**发布新版本**：改 `android/app/build.gradle.kts` 里的 `versionCode` / `versionName`，合并后到 Actions ▸ Android ▸ Run workflow 勾选 publish（或在 main 上推送 `android-v<versionName>` 标签），CI 会自动建 Release 并附上 APK。

**自己编译**：用 Android Studio 打开 `android/` 目录直接运行；或者命令行 `cd android && ./gradlew assembleDebug`。只想跑核心算法的单元测试（不需要 Android SDK）：`./gradlew -Pxingxia.coreOnly=true :core:test`。

功能与 iOS 版基本对齐：今日、占卜、百宝箱全部 12 个工具、记录（心情打卡、倒数日、运势日历、决策历史）、多人档案（含农历生日）、8 套主题 × 深浅色与全部自动切换规则、合成音效、触感与摇一摇、每日运势通知、桌面小组件（今日运势 / 星座运势 / 倒数日）、桌面图标快捷方式、`xingxia://` 深链接。

与 iOS 版的差异：3D 骰子在 Android 上是 2D 动画；附近餐厅（依赖 Apple 地图）暂未移植；Siri / 控制中心对应为桌面快捷方式和小组件。两端农历都按官方口径（紫金历 / 香港天文台）查同一份 1900–2100 年数据表，结果一致（不用系统农历，ICU 在少数年份有误，例如把 2027 年春节算成 2 月 7 日）。

```
android/core/   纯 Kotlin：历法、天文、八字、星座、运势引擎（含单元测试）
android/app/    Compose 界面、主题、存储
```

## 功能一览

**今日**：专属运势（八字日主 × 当日干支、生肖 × 日支、月亮星座 × 太阳星座）、趣味/黄历宜忌、星座短评、今日一签、快捷工具、倒数日、心情打卡、每日一句。模块可排序、隐藏。

**占卜**：摇签筒（60 支原创签诗，摇手机或按住摇签）、塔罗（22 张大阿卡纳、4 种牌阵、3D 翻牌 + 镭射光泽）、答案之书、星座运势（今日/明日/本周/本月）、黄历（农历、干支、节气、建除十二神、黄黑道、冲煞、彭祖百忌、喜神财神）、命盘（四柱八字 + 真太阳时、五行分布、太阳/月亮/上升星座）、缘分配对。

**百宝箱**：今天吃什么（200 道菜品库 / 自定义菜单 / 附近餐厅 + 智能去重、黑名单）、幸运大转盘、自定义抽奖、指尖轮盘、随机分组、随机数字（含双色球/大乐透）、抛硬币、RealityKit 物理 3D 骰子、灵感生成器（配色/中国传统色/密码/名字/字母）、真心话大冒险、刮刮乐、石头剪刀布（含读心模式）。

**记录**：心情打卡（含心情 × 运势相关度）、运势日历与走势图、倒数日（支持农历生日、每年重复、置顶）、决策历史与统计。

**我的**：多人档案（公历/农历生日、出生时辰、出生城市）、8 套主题 × 深浅两版、7 种自动切换规则（每天随机、按时段、按星期、跟随幸运色、跟随星座元素等）+ 低电量与节日彩蛋、音效/震动/摇一摇/每日提醒设置。

**系统集成**：主屏幕与锁屏小组件（今日运势、星座运势、倒数日）、控制中心按钮、Siri 与快捷指令（「星匣今天吃什么」「用星匣抛硬币」等）、每日运势通知、深链接 `xingxia://tool/coin` 等。

## 目录结构

```
XingXia/            App 代码（界面、工具、服务、Siri 意图、资源）
Shared/             App 与小组件共用：天文/历法算法、运势引擎、数据模型、主题、文案库
XingXiaWidgets/     小组件与控制中心按钮
Config/             Info.plist 与 entitlements
```

运势、签文、塔罗、答案、菜品、真心话等文案都是 JSON（`Shared/Content/`、`XingXia/Resources/`），可以直接编辑扩充。

运势与占卜内容为娱乐性质，仅供参考。
