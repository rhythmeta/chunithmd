import Shared
import SwiftUI

struct ScoreQueryView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @AppStorage("scores.grid") private var grid = true
    @AppStorage("scores.sort") private var sort = "rating"
    @AppStorage("scores.ascending") private var ascending = false
    @State private var search = ""
    @State private var filters = ScoreQueryFilters()
    @State private var showingFilters = false
    @State private var response: ScoreQueryResponse?
    @State private var entries: [ScoreQueryEntry] = []
    @State private var songs: [String: CatalogSongViewData] = [:]

    var body: some View {
        Group {
            if let response {
                ScoreQueryResultsView(entries: entries, stats: response.stats, songs: songs, grid: grid)
            } else if catalog.bundle == nil {
                CatalogLoadingView()
            } else {
                ProgressView(tr("正在加载…")).frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle(tr("成绩查询"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(grid ? tr("列表视图") : tr("网格视图"), systemImage: grid ? "list.bullet" : "square.grid.2x2") { grid.toggle() }
                    .labelStyle(.iconOnly).tint(.primary).accessibilityIdentifier("score-query-layout")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu(tr("排序"), systemImage: "arrow.up.arrow.down") {
                    Picker(tr("排序"), selection: $sort) {
                        Label("Rating", systemImage: "star.fill").tag("rating")
                        Label(tr("分数"), systemImage: "number").tag("score")
                        Label(tr("定数"), systemImage: "chart.bar.fill").tag("level")
                    }
                    Divider()
                    Button(ascending ? tr("升序") : tr("降序"), systemImage: ascending ? "arrow.up" : "arrow.down") { ascending.toggle() }
                }.labelStyle(.iconOnly).tint(.primary).accessibilityIdentifier("score-query-sort")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("筛选"), systemImage: filters.shared.isEmpty ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.decrease.circle.fill") { showingFilters = true }
                    .labelStyle(.iconOnly).tint(.primary).accessibilityIdentifier("score-query-filter")
            }
        }
        .searchable(text: $search, placement: .navigationBarDrawer(displayMode: .always), prompt: tr("歌曲、艺术家、别名..."))
        .sheet(isPresented: $showingFilters) { ScoreQueryFilterView(filters: $filters) }
        .task(id: personal.revision) { reload() }
        .onChange(of: catalog.aliases) { reload() }
        .task(id: search) {
            do { try await Task.sleep(for: .milliseconds(150)) } catch { return }
            applyQuery()
        }
        .onChange(of: filters) { applyQuery() }
        .onChange(of: sort) { applyQuery() }
        .onChange(of: ascending) { applyQuery() }
    }

    private func reload() {
        guard let bundle = catalog.bundle else { return }
        do {
            response = try personal.bridge.scoreQuery(bundle: bundle, aliases: catalog.aliases)
            songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
            applyQuery()
        } catch { personal.error = error.localizedDescription }
    }

    private func applyQuery() {
        let mode: ScoreQuerySortMode = switch sort {
        case "score": .score
        case "level": .level
        default: .rating
        }
        entries = ScoreQueryCalculatorKt.filterAndSortScoreQueryEntries(entries: response?.entries ?? [], searchText: search,
            settings: filters.shared, sortMode: mode, ascending: ascending)
    }
}
