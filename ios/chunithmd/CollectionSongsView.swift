import SwiftUI
import Shared

struct CollectionSongsView: View {
    let collectionID: String?
    let title: String
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var shareURL: URL?
    @AppStorage("collections.grid") private var grid = false
    @State private var renaming = false
    @State private var name = ""
    var body: some View {
        let items = personal.snapshot.collectionItems.filter { $0.collectionId == collectionID }
        let favorites = catalog.allSongs.filter { personal.snapshot.favoriteSongIds.contains($0.id) }
        Group {
            if collectionID == nil {
                if favorites.isEmpty { ContentUnavailableView(tr("还没有喜欢的歌曲"), systemImage: "heart") }
                else if grid { CoverGrid(songs: favorites, preferenceKey: "collections.gridColumns") }
                else { List(favorites) { SongRow(song: $0) } }
            } else if items.isEmpty { ContentUnavailableView(tr("还没有谱面"), systemImage: "folder", description: Text(tr("在歌曲详情中选择难度，再加入收藏夹。"))) }
            else if grid {
                let visible = items.filter { item in catalog.allSongs.contains { $0.id == item.songId } }
                CoverGrid(songs: visible.compactMap { item in catalog.allSongs.first { $0.id == item.songId } },
                    captions: visible.map { $0.difficulty.uppercased() }, sheetIDs: visible.map { $0.chartType + ":" + $0.difficulty }, preferenceKey: "collections.gridColumns")
            } else {
                List(items) { item in
                    if let song = catalog.allSongs.first(where: { $0.id == item.songId }) {
                        SongRow(song: song, subtitle: item.difficulty.uppercased(), preferredSheet: item.chartType + ":" + item.difficulty)
                            .swipeActions {
                                Button(tr("移除"), role: .destructive) {
                                    personal.perform(catalog: catalog) { try personal.bridge.toggleCollectionSong(collectionId: item.collectionId, songId: item.songId, type: item.chartType, difficulty: item.difficulty) }
                                }
                            }
                    } else { Text(tr("未收录歌曲：{0}", item.songId)).foregroundStyle(.secondary) }
                }
            }
        }.navigationTitle(personal.snapshot.collections.first { $0.id == collectionID }?.name ?? title)
            .toolbar {
                Button(tr("切换布局"), systemImage: grid ? "list.bullet" : "square.grid.3x3") { grid.toggle() }
                if collectionID != nil {
                    Button(tr("重命名"), systemImage: "pencil") { name = personal.snapshot.collections.first { $0.id == collectionID }?.name ?? title; renaming = true }
                    if let shareURL { ShareLink(item: shareURL).labelStyle(.iconOnly) }
                }
            }
            .task(id: personal.revision) {
                if let collectionID {
                    do { shareURL = URL(string: try personal.bridge.collectionLink(id: collectionID)) }
                    catch { personal.error = error.localizedDescription }
                }
            }
            .alert(tr("重命名收藏夹"), isPresented: $renaming) {
                TextField(tr("名称"), text: $name)
                Button(tr("取消"), role: .cancel) {}
                Button(tr("保存")) { personal.perform(catalog: catalog) { try personal.bridge.saveCollection(id: collectionID, name: name) } }
            }
    }
}
