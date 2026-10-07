import Shared
import SwiftUI

struct RecommendationView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var response: RecommendationResponse?
    @State private var newSongs = true
    @State private var visibleNewCount = 10
    @State private var visibleOldCount = 10

    var body: some View {
        Group {
            if let response {
                if newSongs {
                    RecommendationPageView(entries: response.new_, visibleCount: $visibleNewCount, refresh: reload)
                } else {
                    RecommendationPageView(entries: response.old, visibleCount: $visibleOldCount, refresh: reload)
                }
            } else if catalog.bundle == nil {
                CatalogLoadingView()
            } else {
                ProgressView(tr("正在加载…")).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(AppTheme.page)
        .navigationTitle(tr("吃分推荐"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Picker(tr("推荐范围"), selection: $newSongs) {
                        Text(tr("新曲推荐")).tag(true)
                        Text(tr("旧曲推荐")).tag(false)
                    }
                } label: {
                    Label(newSongs ? tr("新曲推荐") : tr("旧曲推荐"), systemImage: "rectangle.2.swap")
                        .labelStyle(.iconOnly)
                        .font(.body)
                }.tint(.primary).accessibilityIdentifier("recommendation-scope")
            }
        }
        .task(id: personal.revision) { reload() }
    }

    private func reload() {
        guard let bundle = catalog.bundle else { return }
        do {
            response = try personal.bridge.recommendations(bundle: bundle)
            visibleNewCount = 10
            visibleOldCount = 10
        } catch { personal.error = error.localizedDescription }
    }
}
