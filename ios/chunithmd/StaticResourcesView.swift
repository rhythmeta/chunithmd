import SwiftUI

struct StaticResourcesView: View {
    @Bindable var store: CatalogStore

    private var canReinstall: Bool {
        store.manifest != nil && !store.updateAvailable && store.errorMessage == nil
    }

    var body: some View {
        List {
            Section {
                StaticResourceStatusView(store: store)
            }
            Section(tr("更新操作")) {
                if store.isSyncing {
                    VStack(alignment: .leading, spacing: 10) {
                        if let progress = store.syncProgress?.progress {
                            ProgressView(value: progress)
                        } else {
                            ProgressView()
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        Text(store.syncMessage)
                            .font(.footnote).foregroundStyle(.secondary)
                        if let progress = store.syncProgress, progress.stage == "Downloading" {
                            Text(downloadProgressText(progress))
                                .font(.footnote).foregroundStyle(.secondary)
                        }
                    }
                    .padding(.vertical, 4)
                    .accessibilityIdentifier("static-resources-progress")
                } else {
                    StaticResourceActionRow(
                        title: store.updateAvailable ? tr("下载并更新") : canReinstall ? tr("重新安装当前版本") : tr("立即更新"),
                        icon: store.updateAvailable ? "arrow.down.circle" : canReinstall ? "arrow.clockwise.circle" : "arrow.triangle.2.circlepath",
                        color: canReinstall ? .orange : .blue,
                        action: store.refresh)
                        .accessibilityIdentifier("static-resources-download")
                    StaticResourceActionRow(title: tr("重新检查更新"), icon: "magnifyingglass", color: .green) {
                        Task { await store.checkForUpdate() }
                    }
                    .accessibilityIdentifier("static-resources-check")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("静态数据"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(store.isSyncing)
        .interactiveDismissDisabled(store.isSyncing)
        .task { await store.checkForUpdate() }
        .refreshable { await store.checkForUpdate() }
    }
}
