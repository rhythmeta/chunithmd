import SwiftUI
import Shared

struct CollectionSongsView: View {
    let collectionID: String
    let title: String
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var shareURL: URL?
    @State private var items: [PersonalSnapshot.Item] = []
    @AppStorage("collections.grid") private var grid = false
    @AppStorage("collections.sort") private var sort = "default"
    @AppStorage("collections.ascending") private var ascending = true
    @State private var renaming = false
    @State private var name = ""

    var body: some View {
        let songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        Group {
            if items.isEmpty {
                ContentUnavailableView(tr("还没有收藏谱面"), systemImage: "rectangle.stack", description: Text(tr("在歌曲详情中选择难度，再加入收藏夹。")))
            } else if grid {
                let visible = items.filter { songs[$0.songId] != nil }
                CoverGrid(songs: visible.compactMap { songs[$0.songId] },
                          sheetIDs: visible.map { $0.chartType + ":" + $0.difficulty }, preferenceKey: "collections.gridColumns",
                          showsDifficultyBorders: true, onRemove: { remove(visible[$0]) })
            } else {
                List(items) { item in
                    Group {
                        if let song = songs[item.songId] {
                            SongRow(song: song, preferredSheet: item.chartType + ":" + item.difficulty, showsProgress: true, scrollsText: true)
                                .accessibilityIdentifier("collection-chart-" + item.id)
                        } else {
                            Label(tr("未收录歌曲：{0}", item.songId), systemImage: "music.note").foregroundStyle(.secondary)
                        }
                    }
                    .alignmentGuide(.listRowSeparatorLeading) { _ in 0 }
                    .alignmentGuide(.listRowSeparatorTrailing) { $0.width }
                    .swipeActions {
                        Button(tr("移出收藏夹"), systemImage: "trash", role: .destructive) { remove(item) }.tint(.red)
                    }
                }
            }
        }
        .background(AppTheme.page)
        .navigationTitle(personal.snapshot.collections.first { $0.id == collectionID }?.name ?? title)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(grid ? tr("列表") : tr("网格"), systemImage: grid ? "list.bullet" : "square.grid.2x2") { grid.toggle() }
                    .tint(.primary).accessibilityIdentifier("collection-layout")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu(tr("排序"), systemImage: "arrow.up.arrow.down") {
                    Picker(tr("排序"), selection: $sort) {
                        Text(tr("默认顺序")).tag("default")
                        Text(tr("版本 / 发行日期")).tag("versionDate")
                        Text(tr("定数")).tag("difficulty")
                    }
                    Divider()
                    Button { ascending.toggle() } label: {
                        Label(ascending ? tr("升序") : tr("降序"), systemImage: ascending ? "arrow.up" : "arrow.down")
                    }
                }.tint(.primary).accessibilityIdentifier("collection-sort")
            }
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button(tr("重命名收藏夹"), systemImage: "pencil") {
                        name = personal.snapshot.collections.first { $0.id == collectionID }?.name ?? title; renaming = true
                    }
                    if let shareURL { ShareLink(item: shareURL) { Label(tr("分享收藏夹"), systemImage: "square.and.arrow.up") } }
                } label: { Label(tr("收藏夹"), systemImage: "ellipsis.circle") }
                    .tint(.primary).accessibilityIdentifier("collection-actions")
            }
        }
        .task(id: "\(personal.revision):\(catalog.allSongs.count):\(sort):\(ascending)") {
            guard let bundle = catalog.bundle else { return }
            do {
                let json = try personal.bridge.collectionItemsJson(bundle: bundle, id: collectionID, sort: sort, ascending: ascending)
                items = try JSONDecoder().decode([PersonalSnapshot.Item].self, from: Data(json.utf8))
                shareURL = URL(string: try personal.bridge.collectionLink(id: collectionID))
            } catch { personal.error = error.localizedDescription }
        }
        .alert(tr("重命名收藏夹"), isPresented: $renaming) {
            TextField(tr("收藏夹名称"), text: $name)
            Button(tr("取消"), role: .cancel) {}
            Button(tr("保存")) { personal.perform(catalog: catalog) { try personal.bridge.saveCollection(id: collectionID, name: name) } }
                .disabled(name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
    }

    private func remove(_ item: PersonalSnapshot.Item) {
        personal.perform(catalog: catalog) {
            try personal.bridge.toggleCollectionSong(collectionId: item.collectionId, songId: item.songId, type: item.chartType, difficulty: item.difficulty)
        }
    }
}
