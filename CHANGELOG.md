# Changelog

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
