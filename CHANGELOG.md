# Changelog

## 0.5

### 中文

#### 变更与修复

- `d9d0ed4` 修复 Android 启动恢复后未显示应用内更新提示的问题。
- `9441c49` 修复 Android 在设置中手动检查更新时未弹出更新底部面板的问题，并补充对应本地化文案和更新状态处理。
- `912f7f0` 移除 Android 更新流程中的无用日志，调整页面缩放设置中输入框与按钮的间距。
- `a88f941` 稳定 Android 平台桥接和歌曲目录转场，修复扫描器预览、更新检查、账号快照、收藏夹和成绩仓储中的平台实现细节。
- `baae2c6` 清理 Android 代码风格，修正扫描器、歌曲详情和更新页面的实现细节。
- `dc2760a` 修复 iOS 歌曲目录中进度点缺失的问题。
- `8faf200` 调整 iOS 当前最佳成绩卡片的圆角。
- `fa15dc3` 将 iOS 成绩历史日期改为 `yyyy/mm/dd` 格式。
- `112b207` 修复 Android 牌子进度页面边框位置和渐变圆角，并调整相关封面转场。
- `ffeccf5` 修复 Android 定数表、牌子进度和成绩查询中其他带边框曲绘的转场效果。
- `7b816b0` 调整 Android 歌曲列表卡片版本标签的圆角和内边距。
- `b0dcf50` 调整 iOS 当前最佳成绩卡片样式。
- `b1d813d` 修复 Android 液态玻璃导航栏的异常采样问题。
- `41b9b6e` 调整 Android 版本标签的水平内边距。
- `d0f8a36` 修复 Android 歌曲详情中指示标签的形状。
- `0aff03f` 移除 Android 成绩查询中重复的难度表达方式。
- `0346063` 改进 Android 网格模式，调整成绩查询网格布局和封面网格的过度滚动效果。
- `798a8d5` 补全 Android Best 数量的当前总计显示，并调整彩色 Rating 徽章样式和本地化。

### English

#### Changes and Fixes

- `d9d0ed4` Fixed the Android in-app update prompt not appearing after startup recovery.
- `9441c49` Fixed Android manual update checks in Settings failing to show the update bottom sheet, and completed the related localization and update-state handling.
- `912f7f0` Removed unused logging from the Android update flow and adjusted spacing between the page-scale input and button.
- `a88f941` Stabilized Android platform bridges and catalog transitions, including scanner preview, update checks, account snapshots, collections, and score repositories.
- `baae2c6` Cleaned up Android code style and corrected implementation details in scanner, song-detail, and update screens.
- `dc2760a` Fixed missing progress dots in the iOS song catalog.
- `8faf200` Adjusted the corner radius of the iOS personal-best score card.
- `fa15dc3` Formatted iOS score-history dates as `yyyy/mm/dd`.
- `112b207` Fixed Android Plate Progress border placement and gradient corners, and refined the related cover transition.
- `ffeccf5` Fixed transitions for other bordered jackets in Android Constant Table, Plate Progress, and Score Query.
- `7b816b0` Adjusted the Android catalog-list version badge corner radius and padding.
- `b0dcf50` Refined the iOS personal-best score card style.
- `b1d813d` Fixed an incorrect Liquid Glass navigation-bar sampling issue on Android.
- `41b9b6e` Adjusted Android version-badge horizontal padding.
- `d0f8a36` Fixed the Android song-detail indication shape.
- `0aff03f` Removed duplicated difficulty wording from Android Score Query.
- `0346063` Improved Android grid mode, including Score Query grid layout and cover-grid overscroll behavior.
- `798a8d5` Added a current-total display to the Android Best-count settings and refined the multicolor Rating badge style and localization.

## 0.4.1

### 中文

- Android 新增应用内 CI 更新检查，启动后静默检查 main 分支，也可在设置「关于」中手动检查。
- 新版本提示显示当前与新构建的提交数、作者、构建时间和变更记录，并支持查看完整 GitHub 比较及分享 APK 直链。
- 支持后台下载、进度显示、取消、重试和重启后恢复；安装前校验 APK 完整性、包名、版本与签名，并引导授予安装权限。
- CI 将四种 ABI 的裸 APK 发布到 R2，匿名下载并仅保留最新 main 构建；其他分支保留 GitHub Artifacts。

### English

- Added Android in-app CI update checks, with a silent startup check for main and a manual check in Settings → About.
- Update prompts show current and new commit counts, author, build time, and change history, with links to the full GitHub comparison and direct APK sharing.
- Added background downloads with progress, cancel, retry, and restoration after reopening. APK integrity, package, version, and signature are verified before installation, with guidance for installation permission.
- CI publishes raw APKs for all four ABIs to R2 for anonymous downloads and retains only the latest main build. Other branches keep GitHub Artifacts.
