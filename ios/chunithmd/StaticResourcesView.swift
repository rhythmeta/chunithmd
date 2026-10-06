import SwiftUI
import Shared

struct StaticResourcesView: View {
    @Bindable var store: CatalogStore

    var body: some View {
        Form {
            Section(tr("当前版本")) {
                LabeledContent(tr("版本"), value: store.manifest?.version ?? tr("未安装"))
                LabeledContent("SHA-256") {
                    Text(store.manifest?.sha256 ?? "-")
                        .font(.caption.monospaced())
                        .textSelection(.enabled)
                        .multilineTextAlignment(.trailing)
                }
                LabeledContent(tr("更新时间"), value: store.manifest?.createdAt ?? "-")
            }
            Section(tr("数据同步")) {
                LabeledContent(tr("状态"), value: store.syncMessage)
                if let error = store.errorMessage {
                    Text(error).foregroundStyle(.red)
                }
                if let progress = store.syncProgress, progress.stage == "Downloading" {
                    ProgressView(value: progress.progress ?? 0)
                        .progressViewStyle(.linear)
                    Text(downloadProgressText(progress))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                } else if store.isSyncing {
                    ProgressView()
                }
                Button(tr("检查更新"), systemImage: "arrow.clockwise") { store.checkForUpdate() }
                    .disabled(store.isSyncing)
                Button(store.updateAvailable ? tr("下载并更新") : (store.manifest == nil ? tr("下载资源") : tr("重新安装当前版本")), systemImage: "arrow.down.circle") { store.refresh() }
                    .disabled(store.isSyncing)
            }
        }
        .navigationTitle(tr("静态数据"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
