import SwiftUI

struct CatalogLoadingView: View {
    @Environment(CatalogStore.self) private var catalog
    var body: some View {
        ContentUnavailableView {
            Label("CHUNITHM", systemImage: "music.note.list")
        } description: {
            Text(catalog.errorMessage ?? catalog.syncMessage)
            if let progress = catalog.syncProgress {
                ProgressView(value: progress.progress ?? 0)
                Text(downloadProgressText(progress)).font(.caption)
            } else if catalog.isSyncing { ProgressView() }
        } actions: {
            if !catalog.isSyncing {
                Button(tr("下载歌曲资源"), systemImage: "arrow.down.circle") { catalog.refresh() }
                    .buttonStyle(.borderedProminent)
            }
        }
    }
}
