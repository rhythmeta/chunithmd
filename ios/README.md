# chunithmd iOS

SwiftUI 原生客户端，最低支持 iOS 26。沿用项目的 Kotlin Multiplatform、Kotlin 2.4.20、Ktor 3.6.0 和 Swift 6 配置；领域逻辑放在 `android/shared`，iOS 负责界面、导航和系统集成。

## 运行

打开 `chunithmd.xcodeproj`，选择共享的 `chunithmd` scheme 和 iOS 26 或更高版本的模拟器。Xcode 的构建阶段会生成并嵌入 KMP Shared framework。真机运行需配置自己的签名团队。

首次启动会下载曲库及封面资源。后续启动使用本地缓存并检查更新；可在「设置 → 静态资源」手动更新。

## 界面与功能

- 首页按 maimaid iOS 的实际布局对齐：16 点页边距、60 点头像与 Rating 角标、紧凑的 Best 50 入口、渐变图标与双列功能卡片。
- 歌曲搜索是底部独立搜索按钮，进入后使用系统展开的搜索栏；支持别名、ID、排序和筛选。
- 歌曲列表使用 52 点封面、难度色条和版本徽章。筛选使用流式胶囊选项和双滑块。详情沿用封面取色背景、元数据胶囊、地区和外链入口，以及逐张展开的谱面卡片。
- 歌曲详情使用单张封面连续插值移动；支持按钮返回、可取消的边缘返回，以及系统“减弱动态效果”。各个列表、网格和成绩入口独立保存封面来源。
- 网格沿用 Android 的居中方格布局、2 点间距、连续双指缩放及 3/5 列吸附。只创建可见区域附近的单元格，并保持缩放中心附近的歌曲位置。
- 支持玩家档案和头像、成绩录入及历史、Best 50、成绩查询、定数表、随机选曲、推分推荐、牌子进度、歌曲收藏和谱面收藏夹。
- Best 50 和定数表可生成 PNG，再通过系统分享面板导出；大型定数表每 60 个谱面分页，避免超长图片。收藏夹支持 Android/dashboard 兼容的 CHMD1 分享链接。
- 支持 Rhythmeta 账号、社区别名、云备份，以及水鱼、落雪和 Otogame 成绩导入。Otogame 登录使用独立的临时 WKWebView 会话。

## 数据边界

`PersonalDataBridge` 在 KMP 中复用成绩、Rating、推荐、牌子、导入和分享规则。Swift 的 Store 将共享状态转换为界面数据。

iOS 通过 `Localization.swift` 的 `tr` 调用 KMP `AppStrings`。应用启动时使用共享的 `AppLanguage.resolve` 解析系统／应用语言，支持简体中文、繁体中文、英文和日文。翻译只维护在 `localization/strings.json`，带参数的文案使用 `{0}` 等占位符；歌曲名称、用户输入和存储用枚举值不翻译。`scripts/generate-localization.py --check` 同时检查 Swift 文案接入和共享翻译是否同步。

个人数据继续存入既有的 `personal.pb.gz`，由 `RhythmetaSnapshotFiles` 提供受保护的原子文件写入。每次修改读取最新快照，保留其他平台的数据字段；存在恢复日志时拒绝个人数据修改。账号凭据仍存入 Keychain，静态资源独立缓存。

## 验证

在仓库根目录执行：

```sh
./scripts/check-ios-grid.sh
python3 scripts/generate-localization.py --check
cd android
./gradlew :shared:testAndroidHostTest :app:compileDebugKotlin
```

在 Xcode 中用 Product → Test 运行 `chunithmdUITests`。测试覆盖底部搜索、封面详情往返、双指缩放、边缘返回取消/完成，以及定数表图片生成。UI 测试还覆盖搜索键盘收起、谱面卡片展开、设置入口和筛选选项；生成的界面截图供人工对照检查。测试使用模拟器数据，首次运行需要网络下载曲库。

已有验证环境为 iPhone 17 Pro / iOS 26.4。第三方真实账号授权、实际成绩导入和云端备份往返需要使用对应账号验证；自动化测试不提交社区投票，也不修改远端账号数据。

排序回归覆盖三项单选、方向切换和重启后恢复；语言回归使用启动参数分别验证英文、日文和繁体中文的首页、设置和排序菜单。
