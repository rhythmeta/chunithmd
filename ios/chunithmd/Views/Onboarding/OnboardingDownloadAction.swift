import SwiftUI

struct OnboardingDownloadAction: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if catalog.isSyncing {
                ProgressView(value: catalog.syncProgress?.progress)
                Text(catalog.syncMessage).font(.footnote).foregroundStyle(.secondary)
            } else {
                Text(catalog.errorMessage == nil
                     ? tr("先下载曲库和封面，识别模型可在扫描时按需下载。")
                     : tr("资源下载失败，请检查网络后重试。"))
                    .font(.footnote).foregroundStyle(.secondary)
            }
            Button(action: catalog.refresh) {
                Text(catalog.isSyncing ? tr("正在下载资源…") : catalog.errorMessage == nil ? tr("下载资源并继续") : tr("重试下载"))
                    .font(.headline).frame(maxWidth: .infinity).padding(.vertical, 10)
            }
            .buttonStyle(.borderedProminent).buttonBorderShape(.capsule)
            .disabled(catalog.isSyncing)
            .accessibilityIdentifier("onboarding-download")
        }
        .frame(maxWidth: 512).padding(24).frame(maxWidth: .infinity)
        .background(AppTheme.page)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.2), value: catalog.isSyncing)
    }
}
