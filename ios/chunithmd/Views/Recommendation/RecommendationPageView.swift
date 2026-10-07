import Shared
import SwiftUI

struct RecommendationPageView: View {
    let entries: [RecommendationResult]
    @Binding var visibleCount: Int
    let refresh: () -> Void

    var body: some View {
        List {
            if entries.isEmpty {
                ContentUnavailableView(tr("暂时没有可吃分的谱面"), systemImage: "sparkles",
                                       description: Text(tr("先录入成绩，或提高当前谱面的分数")))
                    .frame(maxWidth: .infinity, minHeight: 320).listRowBackground(Color.clear)
            } else {
                ForEach(entries.prefix(visibleCount), id: \.chartId) { entry in
                    RecommendationRow(entry: entry)
                }
                if visibleCount < entries.count {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .listRowSeparator(.hidden)
                    .id(visibleCount)
                    .onAppear { visibleCount = min(visibleCount + 10, entries.count) }
                }
            }
        }
        .listStyle(.insetGrouped)
        .accessibilityIdentifier("recommendation-list")
        .refreshable { refresh() }
    }
}
