import SwiftUI

struct StaticResourceStatusView: View {
    let store: CatalogStore

    private var title: String {
        if let error = store.errorMessage { return tr("检查失败：{0}", error) }
        if store.isSyncing {
            switch store.syncStage {
            case "Downloading": return tr("正在下载静态数据…")
            case "Validating": return tr("正在校验静态数据…")
            case "Applying": return tr("正在应用静态数据…")
            default: return tr("正在检查更新…")
            }
        }
        if store.updateAvailable { return tr("发现可用更新") }
        return store.manifest == nil ? tr("准备检查更新") : tr("已是最新静态数据")
    }

    private var detail: String {
        if store.errorMessage != nil { return tr("请检查网络或后端状态后重试。") }
        if store.isSyncing {
            switch store.syncStage {
            case "Downloading": return tr("正在下载目录和封面资源。")
            case "Validating": return tr("正在校验下载内容。")
            case "Applying": return tr("正在保存本地静态数据。")
            default: return tr("正在从后端获取最新清单。")
            }
        }
        if store.updateAvailable { return tr("点击下方按钮下载并应用完整更新。") }
        return store.manifest == nil ? tr("进入页面后会自动检查静态数据更新。") : tr("当前本地数据与服务端最新版本一致。")
    }

    private var icon: String {
        if store.errorMessage != nil { return "xmark.octagon.fill" }
        if store.isSyncing { return "arrow.triangle.2.circlepath.circle.fill" }
        if store.updateAvailable { return "arrow.down.circle.fill" }
        return store.manifest == nil ? "arrow.triangle.2.circlepath.circle.fill" : "checkmark.circle.fill"
    }

    private var color: Color {
        if store.errorMessage != nil { return .red }
        if store.isSyncing || store.updateAvailable || store.manifest == nil { return .blue }
        return .green
    }

    private var versionText: String? {
        guard !store.isSyncing, store.errorMessage == nil,
              let manifest = store.checkedManifest ?? store.manifest else { return nil }
        let date = (try? Date(manifest.createdAt, strategy: .iso8601))
            ?? (try? Date(manifest.createdAt, strategy: Date.ISO8601FormatStyle(includingFractionalSeconds: true)))
        let time = date?.formatted(date: .numeric, time: .shortened) ?? tr("未知时间")
        return tr("版本：{0} · 构建：{1}", manifest.version, time)
    }

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 44, weight: .semibold))
                .foregroundStyle(color)
                .accessibilityHidden(true)
            Text(title).font(.title3.bold())
            Text(detail).font(.subheadline).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
            if let versionText {
                Text(versionText).font(.caption).foregroundStyle(.secondary)
                    .textSelection(.enabled)
            }
        }
        .multilineTextAlignment(.center)
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("static-resources-status")
    }
}
