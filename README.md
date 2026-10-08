# chunithmd

简体中文 | [English](README_en.md)

面向 CHUNITHM 玩家的 Android / iOS 助手，提供曲库查询、成绩管理、Rating 分析和相机识别。属于 Rhythmeta 生态，使用 Kotlin Multiplatform 共享业务逻辑，搭配 Android Miuix 与 iOS SwiftUI 原生界面。

## 功能

- **曲库查询**：按曲名、别名或 ID 搜索，支持排序、筛选、谱面详情及日服、国际服、国服数据。
- **成绩与档案**：管理多个玩家档案，手动记录成绩、查看游玩历史，计算单曲 Rating 和 Best 50。
- **选曲辅助**：定数表、成绩查询、随机选曲、推分推荐和牌子进度。
- **收藏与分享**：收藏歌曲、建立谱面收藏夹，通过 `CHMD1` 链接分享；将 Best 50 和定数表导出为图片。
- **扫描识别**：竖持相机识别选曲画面的歌曲，横持识别成绩；也可从相册导入图片，核对后保存成绩。
- **导入与云服务**：支持水鱼、落雪和 Otogame 成绩导入，以及 Rhythmeta 账号、社区别名和手动云备份。
- **多语言**：支持简体中文、繁体中文、英文和日文。

## 获取应用

| 平台 | 最低系统要求 | 构建产物 |
| --- | --- | --- |
| Android | Android 10（API 29） | [APK 构建工作流](https://github.com/rhythmeta/chunithmd/actions/workflows/build-apk.yml)，按 CPU 架构提供 APK |
| iOS | iOS 26 | [IPA 构建工作流](https://github.com/rhythmeta/chunithmd/actions/workflows/build-ipa.yml)，提供未签名 IPA |

在成功的工作流运行页面中下载 Artifacts。iOS 的 IPA 需要自行签名后安装，也可以从源码通过 Xcode 运行。

首次启动时，按引导下载曲库和封面，再在设置中选择档案对应的服务器。曲库缓存后可供离线查询；扫描模型在首次使用扫描功能时单独下载，不包含在安装包中。模型下载并校验完成后，识别在设备上进行，扫描图片不会上传。

扫描结果需要核对并明确保存才会写入游玩记录；FC / AJ / AJC 等状态请对照原图确认。云备份需要手动创建，恢复备份会替换本机该游戏的个人数据。

## 从源码构建

```sh
git clone https://github.com/rhythmeta/chunithmd.git
cd chunithmd
```

两端构建号默认取 Git 提交总数，建议保留完整 Git 历史。需要指定构建号时，可设置正整数环境变量 `CHUNITHMD_BUILD_NUMBER`。

### Android

准备 JDK 21、Android SDK，以及能够支持仓库所用 Android Gradle Plugin 的 Android Studio。项目当前使用 Kotlin 2.4.20、AGP 9.5.0-alpha05 和 Gradle 9.8.0；通过仓库自带的 Gradle Wrapper 构建。

安装 SDK 组件，并通过 `ANDROID_HOME` 或 `android/local.properties` 中的 `sdk.dir` 配置 SDK 路径：

```sh
sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools"
cd android
./gradlew :app:assembleDebug
```

产物位于 `android/app/build/outputs/apk/debug/app-debug.apk`。也可在 Android Studio 中打开 `android/`，选择 `app` 运行。

Release 构建使用 `:app:assembleRelease`；签名读取 `ANDROID_KEYSTORE_PATH`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS` 和 `ANDROID_KEY_PASSWORD`，密钥库格式为 PKCS12。添加 `-PCHUNITHMD_SPLIT_RELEASE_APKS=true` 可按 CPU 架构拆分 APK。

### iOS

准备 macOS、支持本项目和 iOS 26 及以上目标设备的 Xcode，以及上述 JDK 21 和 Android SDK。iOS 构建也会配置共享模块的 Android 目标，因此需要 Android 构建环境。

1. 打开 `ios/chunithmd.xcodeproj`。
2. 选择 `chunithmd` scheme 和模拟器或真机；共享层支持 Apple Silicon 模拟器与 ARM64 真机。
3. 真机运行时配置自己的签名团队，然后构建运行。

Xcode 构建阶段会自动生成并嵌入 KMP `Shared` framework。更多平台说明见 [iOS README](ios/README.md)。

## 项目结构

```text
android/
  app/                 Android 应用、Miuix 界面及平台适配
  shared/              KMP 领域模型、计算、数据访问和共享状态
ios/                   SwiftUI 应用、系统集成及 UI 测试
shared/                跨端备份与收藏分享的 Protobuf 协议
localization/          四种语言的统一翻译源
src/static-bundle/     曲库数据合并、标准化与静态资源构建
static-worker/         曲库和封面的 Cloudflare 发布配置
model-assets/          扫描模型源文件与平台导出资源
models-worker/         扫描模型的独立发布配置
scripts/               数据、模型、本地化和打包工具
test/                  静态资源与 iOS 辅助脚本测试
```

平台无关的计算、校验、网络请求和业务状态放在 `android/shared/src/commonMain`；原生层负责界面、导航、相机、系统权限及存储适配。Android 使用 Room / DataStore，iOS 使用受保护的快照文件；账号凭据分别由 Keystore / Keychain 适配层管理。

## 开发与验证

以下命令从仓库根目录执行。静态资源工具使用 Node.js 22 和 pnpm 10.28.2，本地化工具使用 Python 3。

```sh
pnpm install --frozen-lockfile
pnpm typecheck
pnpm test
python3 scripts/generate-localization.py --check
```

共享逻辑测试和 Android 编译：

```sh
cd android
./gradlew :shared:testAndroidHostTest :app:compileDebugKotlin
```

iOS UI 测试可在 Xcode 中通过 Product → Test 运行。修改翻译时，编辑 `localization/strings.json`，运行 `python3 scripts/generate-localization.py`，并一同提交生成的 `Translations.kt`。

### 曲库与扫描模型

客户端默认使用已发布的资源，日常构建应用无需重新生成曲库或模型。曲库构建会联网获取上游数据与封面，合并落雪曲库及别名数据，并校验输出：

```sh
pnpm build:static-bundle
```

产物写入 `static-worker/public/`，构建报告写入 `artifacts/static-bundle-report.json`。`pnpm verify:static-bundle` 校验已发布的远端资源；发布目标可通过 `CHUNITHMD_STATIC_BASE_URL` 配置。数据源和其他环境变量见 [构建入口](scripts/build-static-bundle.ts) 与 [默认数据源](src/static-bundle/build.ts)。

Android 扫描使用 ONNX 检测模型与 PaddleOCR；iOS 使用 Core ML 检测模型与系统 Vision。模型独立发布，下载时校验文件大小及 SHA-256。导出、校验和资源构建步骤见 [模型说明](model-assets/README.md)。

## 许可证

本项目采用 [GNU GPL v3](LICENSE)。第三方模型及相关资源的许可说明随对应资源提供，例如 [PaddleOCR 许可](android/app/src/main/assets/scanner/PaddleOCR-LICENSE.txt) 和 [来源说明](android/app/src/main/assets/scanner/PaddleOCR-NOTICE.txt)。
