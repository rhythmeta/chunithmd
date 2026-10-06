import Shared
import SwiftUI

struct ScoreQueryView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @AppStorage("scores.grid") private var grid = false
    @State private var search = ""
    @State private var difficulty = "all"
    @State private var sort = "rating"
    var body: some View {
        let entries = personal.best.filter { entry in
            (search.isEmpty || entry.title.localizedStandardContains(search)) && (difficulty == "all" || entry.difficulty == difficulty)
        }.sorted { sort == "score" ? $0.score > $1.score : sort == "constant" ? $0.constant > $1.constant : $0.rating > $1.rating }
        Group {
            if grid {
                let visible = entries.filter { entry in catalog.allSongs.contains { $0.id == entry.songId } }
                CoverGrid(songs: visible.compactMap { entry in catalog.allSongs.first { $0.id == entry.songId } },
                    captions: visible.map { "\($0.difficulty.uppercased()) \($0.rank)" },
                    sheetIDs: visible.map { $0.type + ":" + $0.difficulty }, preferenceKey: "scores.gridColumns")
            } else {
                List {
                    Section(tr("{0} 个谱面", entries.count)) {
                        ForEach(entries, id: \.chartId) { entry in
                            if let song = catalog.allSongs.first(where: { $0.id == entry.songId }) {
                                SongRow(song: song, subtitle: "\(entry.difficulty.uppercased()) · \(Int(entry.score).formatted()) \(entry.rank) · \(entry.rating.formatted(.number.precision(.fractionLength(2))))", preferredSheet: entry.type + ":" + entry.difficulty)
                            }
                        }
                    }
                }
            }
        }.navigationTitle(tr("成绩查询"))
            .toolbar {
                Menu(tr("筛选与排序"), systemImage: "line.3.horizontal.decrease") {
                    Picker(tr("难度"), selection: $difficulty) {
                        Text(tr("全部")).tag("all")
                        ForEach(difficultyNames, id: \.self) { Text($0.uppercased()).tag($0) }
                    }
                    Picker(tr("排序"), selection: $sort) {
                        Text("Rating").tag("rating")
                        Text(tr("分数")).tag("score")
                        Text(tr("定数")).tag("constant")
                    }
                }
                Button(tr("切换布局"), systemImage: grid ? "list.bullet" : "square.grid.3x3") { grid.toggle() }
            }
            .searchable(text: $search, prompt: tr("搜索已游玩曲目"))
            .overlay { if entries.isEmpty { ContentUnavailableView(tr("没有符合条件的成绩"), systemImage: "list.bullet.rectangle") } }
    }
}
