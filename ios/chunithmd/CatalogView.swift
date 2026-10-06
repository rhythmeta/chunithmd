import Shared
import SwiftUI

struct CatalogView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @AppStorage("catalog.grid") private var grid = false
    @AppStorage("catalog.gridColumns") private var columns = 5
    @State private var filters = false

    private var songs: [CatalogSongViewData] {
        catalog.songs
    }

    var body: some View {
        @Bindable var catalog = catalog
        Group {
            if catalog.bundle == nil { CatalogLoadingView() }
            else if songs.isEmpty { ContentUnavailableView.search(text: catalog.search) }
            else if grid { CoverGrid(songs: songs) }
            else {
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(songs) { song in SongRow(song: song, card: true) }
                    }.padding(.horizontal, 16).padding(.vertical, 8)
                }
            }
        }
        .background(.background)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle(tr("歌曲"))
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Menu(tr("显示选项"), systemImage: grid ? "list.bullet" : "square.grid.2x2") {
                    Picker(tr("布局"), selection: $grid) {
                        Label(tr("列表"), systemImage: "list.bullet").tag(false)
                        Label(tr("网格"), systemImage: "square.grid.3x3").tag(true)
                    }
                    if grid {
                        Picker(tr("网格密度"), selection: $columns) { Text(tr("3 列")).tag(3); Text(tr("5 列")).tag(5) }
                    }
                }.accessibilityIdentifier("catalog-options").tint(.primary)
                Menu(tr("排序"), systemImage: "arrow.up.arrow.down") {
                    Picker(tr("排序方式"), selection: $catalog.sort.animation(.easeInOut)) {
                        Text(tr("默认")).tag("default")
                        Text(tr("版本/日期")).tag("versionDate")
                        Text(tr("难度")).tag("difficulty")
                    }
                    Divider()
                    Button {
                        withAnimation(.easeInOut) { catalog.ascending.toggle() }
                    } label: {
                        Label(catalog.ascending ? tr("升序") : tr("降序"), systemImage: catalog.ascending ? "arrow.up" : "arrow.down")
                    }
                }.accessibilityIdentifier("catalog-sort").tint(.primary)
                Button(tr("筛选"), systemImage: "line.3.horizontal.decrease.circle") { filters = true }.tint(.primary)
            }
        }
        .sheet(isPresented: $filters) { CatalogFilterSheet(store: catalog) }
    }
}
