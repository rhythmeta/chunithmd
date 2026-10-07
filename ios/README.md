# chunithmd iOS

SwiftUI 原生客户端，最低支持 iOS 26。沿用项目的 Kotlin Multiplatform、Kotlin 2.4.20、Ktor 3.6.0 和 Swift 6 配置；领域逻辑放在 `android/shared`，iOS 负责界面、导航和系统集成。

## 运行

打开 `chunithmd.xcodeproj`，选择共享的 `chunithmd` scheme 和 iOS 26 或更高版本的模拟器。Xcode 的构建阶段会生成并嵌入 KMP Shared framework。真机运行需配置自己的签名团队。

首次启动会下载曲库及封面资源。后续启动使用本地缓存并检查更新；可在「设置 → 静态资源」手动更新。

## 目录结构

`chunithmd/` 沿用 maimaid iOS 的分类方式：

- `MyApp.swift`：应用入口；资源目录和应用图标也保留在根目录。
- `Models/`：共享层返回的 Swift 数据模型与界面数据结构。
- `Services/`：KMP 状态桥接、原生存储适配和封面取色。
- `Utils/`：本地化、主题及跨页面使用的样式工具。
- `Views/`：按功能划分为 `Home`、`Catalog`、`Song`、`Best`、`Collections`、`Community`、`ConstantTable`、`Plate`、`Random`、`Recommendation`、`Scanner`、`Score`、`ScoreQuery` 和 `Settings`。设置目录包含档案、静态数据、云端账户和成绩导入页面。
- `Views/Components/`：跨页面复用的头像、封面网格、徽章、筛选控件和分享图片组件。
- `Views/Navigation/`：底部导航、页面路由及歌曲转场。

Xcode 使用同步文件夹自动收录这些目录中的源文件。UI 测试保留在同级 `chunithmdUITests/`，业务逻辑继续维护在 KMP `android/shared` 中。

## 界面与功能

- 底部导航依次为主页、扫描、歌曲和设置，歌曲页保留搜索入口。扫描支持选择单张成绩图、端侧识别、核对谱面和分数后保存；收藏夹从主页进入，收藏分享链接会直接打开该页面。

- 首页按 maimaid iOS 的实际布局对齐：16 点页边距、60 点头像与 Rating 角标、紧凑的 Best 50 入口、渐变图标与双列功能卡片。
- 歌曲位于底部第三个标签，使用系统搜索栏；支持别名、ID、排序和筛选。
- 歌曲列表使用 52 点封面、难度色条和版本徽章。筛选使用流式胶囊选项和双滑块。详情沿用封面取色背景、元数据胶囊、地区和外链入口，以及逐张展开的谱面卡片。
- 歌曲详情使用单张封面连续插值移动；支持按钮返回、可取消的边缘返回，以及系统“减弱动态效果”。各个列表、网格和成绩入口独立保存封面来源。
- 网格沿用 Android 的居中方格布局、2 点间距、连续双指缩放及 3/5 列吸附。只创建可见区域附近的单元格，并保持缩放中心附近的歌曲位置。
- 支持玩家档案和头像、成绩录入及历史、Best 50、成绩查询、定数表、随机选曲、推分推荐、牌子进度、歌曲收藏和谱面收藏夹。
- Best 50 和定数表可生成 PNG，再通过系统分享面板导出；大型定数表每 60 个谱面分页，避免超长图片。收藏夹支持 Android/dashboard 兼容的 CHMD1 分享链接。
- 支持 Rhythmeta 账号、社区别名、云备份，以及水鱼、落雪和 Otogame 成绩导入。Otogame 登录使用独立的临时 WKWebView 会话。

## 数据边界

两端应用均不内置模型。扫描页首次使用时下载模型，显示下载大小、进度、取消与重试；完整校验后可离线使用，有更新时保留当前可用版本直到新版本下载完成。iOS 下载 `ScoreDetector.mlpackage` 并在设备上编译、缓存 `mlmodelc`，定位 title、difficulty、level、score、clear、combo，再用系统 Vision 识别文字。Android 使用同一检测权重的 ONNX 和按需下载的 PaddleOCR v6 small 文字识别模型，支持 ASCII、中文、日文；iOS 不打包 PaddleOCR。图片不会上传。图像方向校正、模型推理和 OCR 在原生后台执行；框解码、文本解析、按档案地区匹配曲库及保存校验由 KMP `shared/scanner` 复用。

模型源文件放在 `model-assets/`（不属于应用资源）。`node scripts/build-model-assets.mjs` 生成 Android/iOS 各自的清单与按 SHA-256 寻址的文件；独立的 `models-worker` 发布到 `https://chunithmd-models.rhythmeta.org`，由 `.github/workflows/build-model-assets.yml` 自动部署和校验。`scripts/export-score-detector.py`、`scripts/fetch-paddleocr.py` 的导出目标也在该目录。KMP `ScannerModelRepository` 负责白名单路径、尺寸与 SHA-256 校验、流式下载和原子激活，`ScannerModelManager` 维护两端共用的下载状态。缓存不进入个人数据备份；已完成的文件可在重试时复用。

WE 的 level 只取属性字符，例如「狂」「止」，不取星级。同名 WE 谱面按属性匹配；结果不明确时需要手动选择。由于未点亮的灰色 FULL COMBO 也能被 OCR 读出，页面展示原始状态文字，COMBO 默认空白，由用户对照原图选择 FC／AJ／AJC。CLEAR 预填仅为 CLEAR／FAILED，不推断技能条状态。每次识别都要明确点击保存才写入当前档案的游玩记录。

更换权重时，在仓库根目录运行 `python3 scripts/export-score-detector.py model-assets/exp.pt`。导出环境见脚本顶部；Core ML 导出在 macOS 执行。两端固定使用 RGB 1024×1024、114 灰色居中 letterbox、原始 `[1,10,21504]` 输出，类别顺序不可改，输出不含 objectness 和 NMS。脚本在导出后验证两种格式，更新资源及 `scripts/score-detector.json` 中的版本、源权重和资源 SHA-256；`python3 scripts/export-score-detector.py --check` 可检查资源完整性。更改输入大小或导出结构时需同步调整共享解码器。

Android OCR 模型来自官方 `PaddlePaddle/PP-OCRv6_small_rec_onnx`，版本与 SHA-256 固定在 `scripts/paddleocr-v6.json`。运行 `python3 scripts/fetch-paddleocr.py` 下载并验证模型、从官方词表生成 Unicode JSON（需 PyYAML、onnx）；`--check` 仅用标准库检查资源。预处理为 BGR、48 像素高、保留比例并右侧补零，使用 CTC 解码。Android ONNX Runtime 固定为 1.30.0，避免旧版在 SM8850 上误用 SME2 指令导致 SIGILL。

Android 扫描页沿用 maimaid 的全屏相机布局：右上角进入相册，底部结果卡片直接打开与手动记录共用的成绩录入面板，相机快门只保存照片。Android 扫描页面保持竖屏布局，提示横持手机；模型分析帧和照片按物理握持方向旋转，与页面方向独立。离开扫描页恢复系统方向设置。CameraX 在前台扫描页启用，申请 1920×1080 分析帧；首个有效谱面和分数立即展示卡片，后续变化按谱面身份与分数做连续帧确认，打开录入面板、处理照片及离开页面时暂停分析。相册结果保留到手动返回实时扫描，保存成绩仍需确认；iOS 同样使用 maimaid 式的全屏相机、右上角相册入口和底部成绩卡片，点击卡片进入通用 `ScoreEntryView`，预填分数和 CLEAR，不再手动选择谱面。页面保持竖向布局，提示横持手机；通过 Core Motion 重力方向判断实际横持状态（包括系统锁定旋转时），原始 1080p 相机帧按左右握持方向转正，每次只处理一帧。切换标签、后台、选择相册或打开成绩录入面板时停止相机分析。“设置 → 外观 → 显示识别框”默认关闭，开启后在相机及相册预览中叠加字段框，按物理方向、aspect-fill 裁切及 aspect-fit 留白换算位置；框随当前检测帧更新，空帧、切换方向及离开扫描时清除。快门按钮保存当前识别卡片对应的照片，按需申请相册添加权限。录入时由共享层再次校验档案、地区、谱面及分数，并保存 CHAIN。

`PersonalDataBridge` 在 KMP 中复用成绩、Rating、推荐、牌子、导入和分享规则。Swift 的 Store 将共享状态转换为界面数据。

iOS 通过 `Utils/Localization.swift` 的 `tr` 调用 KMP `AppStrings`。应用启动时使用共享的 `AppLanguage.resolve` 解析系统／应用语言，支持简体中文、繁体中文、英文和日文。翻译只维护在 `localization/strings.json`，带参数的文案使用 `{0}` 等占位符；歌曲名称、用户输入和存储用枚举值不翻译。`scripts/generate-localization.py --check` 同时检查 Swift 文案接入和共享翻译是否同步。

Best 表沿用 maimaid 的分组列表、版本选择弹层和容量输入布局。版本覆盖仅在当前页面有效；B/N 容量由 KMP 校验并保存到 portable snapshot，复用既有 `android.chunithmd.best.*_count` 备份键。`BestTableResponse` 在共享层生成分组、Rating 和均值，界面与分享使用同一份结果。成绩行展示 Rank、分数、状态徽章、单曲 Rating 和定数，并接入歌曲详情的原生缩放转场。

个人数据继续存入既有的 `personal.pb.gz`，由 `RhythmetaSnapshotFiles` 提供受保护的原子文件写入。每次修改读取最新快照，保留其他平台的数据字段；存在恢复日志时拒绝个人数据修改。账号凭据仍存入 Keychain，静态资源独立缓存。

## 验证

在仓库根目录执行：

```sh
./scripts/check-ios-grid.sh
./scripts/check-ios-scanner-orientation.sh
python3 scripts/generate-localization.py --check
cd android
./gradlew :shared:testAndroidHostTest :app:compileDebugKotlin
```

在 Xcode 中用 Product → Test 运行 `chunithmdUITests`。测试覆盖底部搜索、封面详情往返、双指缩放、边缘返回取消/完成，以及定数表图片生成。UI 测试还覆盖搜索键盘收起、谱面卡片展开、设置入口和筛选选项；生成的界面截图供人工对照检查。测试使用模拟器数据，首次运行需要网络下载曲库。

已有验证环境为 iPhone 17 Pro / iOS 26.4。第三方真实账号授权、实际成绩导入和云端备份往返需要使用对应账号验证；自动化测试不提交社区投票，也不修改远端账号数据。

排序回归覆盖三项单选、方向切换和重启后恢复；语言回归使用启动参数分别验证英文、日文和繁体中文的首页、设置和排序菜单。
