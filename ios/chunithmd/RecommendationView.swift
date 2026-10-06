import SwiftUI
import Shared

struct RecommendationView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @State private var response: RecommendationResponse?
    @State private var newSongs = true
    var body: some View {
        List {
            let entries = newSongs ? response?.new_ ?? [] : response?.old ?? []
            if entries.isEmpty {
                ContentUnavailableView(tr("暂无推荐"), systemImage: "sparkles", description: Text(tr("录入成绩后，为你寻找下一个 Rating 目标。")))
                    .frame(minHeight: 320).listRowBackground(Color.clear)
            }
            ForEach(entries, id: \.chartId) { entry in
                if let song = catalog.allSongs.first(where: { $0.id == entry.song.songId }) {
                    SongRow(song: song, subtitle: "\(entry.sheet.difficulty.uppercased()) → \(entry.targetRank) · +\(entry.potentialGain.formatted(.number.precision(.fractionLength(3))))", preferredSheet: entry.sheet.type + ":" + entry.sheet.difficulty)
                }
            }
        }.navigationTitle(tr("推分推荐"))
            .toolbar {
                Menu(newSongs ? tr("新曲") : tr("旧曲"), systemImage: "rectangle.2.swap") {
                    Picker(tr("推荐范围"), selection: $newSongs) { Text(tr("新曲")).tag(true); Text(tr("旧曲")).tag(false) }
                }
            }
            .task(id: personal.revision) {
                guard let bundle = catalog.bundle else { return }
                do { response = try personal.bridge.recommendations(bundle: bundle) } catch { personal.error = error.localizedDescription }
            }
    }
}
