# Autox.js v7

<p align="center">

![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/aiselp/AutoX/total)
![GitHub Issues or Pull Requests](https://img.shields.io/github/issues/aiselp/AutoX)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/aiselp/AutoX/android-test.yml)
![GitHub Release](https://img.shields.io/github/v/release/aiselp/AutoX)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/ca72518c8bd548f9a350d5a15e2ed9ea)](https://app.codacy.com/gh/aiselp/AutoX/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

</p>

[English Document](README_en.md)

## 简介

Autox.js 是一个运行在 Android 平台上的 JavaScript 自动化与开发环境，支持无障碍服务、脚本运行、界面分析、悬浮窗、打包 APK 等能力，目标是成为更现代、更易扩展的 Auto.js 生态分支。

本项目基于 [hyb1996/Auto.js](https://github.com/hyb1996/Auto.js) 演进而来，在原 4.1 版本基础上持续升级维护，目前重点方向包括：

- 更现代的 v7 UI 与开发体验
- 更完善的打包、签名与模板壳流程
- 更强的 Node.js / TypeScript / Shizuku 能力支持

## 相关链接

- 文档站：<https://autox-doc.vercel.app/>
- 开源地址：<https://github.com/aiselp/AutoX/>
- Releases：<https://github.com/aiselp/AutoX/releases>
- VS Code 插件：[aaroncheng.auto-js-vsce-fixed](https://marketplace.visualstudio.com/items?itemName=aaroncheng.auto-js-vsce-fixed)
- 官方论坛：[www.autoxjs.com](http://www.autoxjs.com)
- 更新日志：[CHANGELOG.md](CHANGELOG.md)

如果下载较慢，可以复制 Release Assets 中 APK 的下载地址，粘贴到 <http://toolwa.com/github/> 等 GitHub 加速站点下载。

## APK 版本说明

当前主要提供以下构建形态：

- `arm64-v8a`：64 位 ARM 设备，默认构建版本
- `mini-arm64-v8a`：进一步精简的版本

当前默认签名产物会额外复制到：

```text
~/Downloads/Autox-v7s-arm64-v8a-release-v<version>-<gitver>.apk
```

## 功能亮点

### 核心能力

1. 基于无障碍服务提供自动点击、长按、滑动等自动化能力。
2. 支持悬浮窗录制、运行与交互控制。
3. 提供强大的选择器 API，可用于查找、遍历、读取和操作屏幕控件。
4. 以 JavaScript 作为脚本语言，并支持补全、重命名、格式化、查找替换等 IDE 能力。
5. 支持使用 e4x 编写界面，并可将 JavaScript 项目打包为 APK。
6. 支持 Root 场景下更强的点击、滑动、录制和 shell 能力。
7. 提供截图、保存图片、找色、找图等图像相关函数。
8. 可作为 Tasker 插件使用，适合自动化工作流。
9. 内置界面分析工具，可分析布局层次、区域范围与控件信息。

### 相比传统按键精灵类工具

1. 更偏向自动化与工作流场景，适合日常效率任务。
2. 控件驱动方式兼容性更好，不容易因分辨率变化失效。
3. 大部分功能不依赖 Root，只有精确坐标点击、滑动等场景才需要。
4. 不只是脚本执行器，还可以承担界面开发和 APK 打包工作。

### v7 版本特性

- [x] 全新基于 Material Design 3 的 UI 界面
- [x] 支持 [Shizuku](https://shizuku.rikka.app/introduction/) 并可运行嵌入式脚本，方便动态调试相关 API
- [x] 引入新的 [Node.js 引擎](https://github.com/caoccao/Javet?tab=readme-ov-file)，支持更多 npm 包能力
- [x] 大量模块迁移到 TypeScript，并补充类型声明
- [x] 全新基于 Vue 3 和 Jetpack Compose 的 UI 框架
- [ ] 新一代基于 Node.js 的 v7 API 仍在持续完善
- [x] 打包与签名能力增强，支持 Node.js 引擎脚本与更多权限配置
- [x] [Rhino](https://github.com/mozilla/rhino/) 升级到 v1.8.0 稳定版

## 示例

示例脚本位于 [app/src/main/assets/sample](app/src/main/assets/sample)，也可以直接在应用内查看和运行。

## 构建与签名

### 环境要求

- JDK 17
- Android SDK / Build Tools
- Node.js 20+

命令默认在项目根目录执行。如果使用 Windows PowerShell 7.0 以下版本，请将链式命令中的 `&&` 改为 `;`。

### 首次准备

先构建 JS 模块：

```shell
./gradlew autojs:buildJsModule
```

这一步通常只需执行一次；如果修改了相关模块代码，需要重新执行。

如果需要生成文档：

```shell
./gradlew app:buildDocs
```

### 常用构建命令

本地调试构建并安装：

```shell
./gradlew app:assembleV7Debug && ./gradlew app:installV7Debug
```

调试包输出目录：

```text
app/build/outputs/apk/v7/debug
```

本地构建未签名发布包：

```shell
./gradlew app:assembleV7Release
```

未签名发布包输出目录：

```text
app/build/outputs/apk/v7/release
```

### 使用 Makefile 生成签名包

```shell
make
```

或：

```shell
make app-signed
```

默认行为：

- 构建 `v7 release`
- 生成或复用 `build/keystore/autox-self.jks`
- 输出签名 APK 到 `build/signed/`
- 额外复制一份到 `~/Downloads/Autox-v7s-arm64-v8a-release-v<version>-<gitver>.apk`

如果只想构建未签名包：

```shell
make unsigned
```

如果需要单独构建 `inrt` 模板壳：

```shell
make inrt-signed
```

### Android Studio 调试与签名

直接选择 `v7Debug` 运行即可。

如果你需要测试内嵌模板壳资源，请先执行：

```shell
./gradlew app:buildDebugTemplateApp
./gradlew app:assembleV7Debug -PincludeTemplateAssets=true -PincludeCodeEditorAssets=true
```

如果你想在 Android Studio 中生成签名发布包：

1. 点击 `Build`
2. 选择 `Generate Signed Bundle / APK...`
3. 选择 `APK`
4. 选择或新建证书
5. 选择 `v7Release`
6. 完成构建

### 关于 template.apk 和 codeeditor

默认发布包不再内嵌 `template.apk` 和 `codeeditor` 资源，以减小 APK 体积。

如果确实需要重新打进 APK，可使用：

```shell
./gradlew :app:assembleV7Release -PincludeTemplateAssets=true -PincludeCodeEditorAssets=true
```

## 测试

目前 `autojs` 模块已添加部分脚本功能测试。运行方式如下：

1. 准备一台 Android 设备，并通过 `adb` 连接电脑。
2. 使用最新版 Android Studio 或命令行完成一次模块构建：`./gradlew autojs:assemble`
3. 打开 `autojs/src/androidTest` 目录下的测试类。
4. 点击类名旁边的运行按钮开始测试。
5. 根据设备情况，可能需要在手机上手动允许测试 APK 安装。

## 许可证

本项目基于上游项目继续演进，使用时请同时关注以下协议与限制：

- 上游相关说明：`MPL-2.0 + 非商业性使用`
- 本项目采用：[GPL-2.0](https://opensource.org/license/gpl-2-0/)

参考链接：

- GPL-2.0：<https://opensource.org/license/gpl-2-0/>
- MPL-2.0：<https://www.mozilla.org/MPL/2.0>
