import Shared
import SwiftUI

struct RandomSongView: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var songs: [CatalogSongViewData] = []
    @State private var displayed: [CatalogSongViewData] = []
    @State private var count: Int32 = 3
    @State private var filters = false
    @State private var spinning = false
    @State private var spinTask: Task<Void, Never>?

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Picker(tr("曲目数"), selection: $count) {
                    Text(tr("3 首")).tag(Int32(3)); Text(tr("4 首")).tag(Int32(4))
                }.pickerStyle(.segmented).padding(.horizontal, 40).padding(.top, 16)
                HStack(spacing: 8) {
                    ForEach(0..<Int(count), id: \.self) { index in
                        RandomSlotView(song: displayed.indices.contains(index) ? displayed[index] : nil, spinning: spinning)
                    }
                }
                .frame(height: count == 3 ? 200 : 170).padding(16)
                .background(AppTheme.surface, in: .rect(cornerRadius: 24)).padding(.horizontal, 16)
                Button(action: draw) {
                    Label(spinning ? tr("跳过动画") : tr("开始抽曲"), systemImage: spinning ? "forward.end.fill" : "dice.fill")
                        .font(.headline.bold()).frame(maxWidth: .infinity).padding(.vertical, 8)
                }.buttonStyle(.borderedProminent).disabled(catalog.songs.isEmpty).padding(.horizontal, 40)
                if !spinning && !songs.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(tr("抽选结果")).font(.system(size: 13, weight: .bold)).foregroundStyle(.secondary).padding(.horizontal, 8)
                        ForEach(songs) { song in SongRow(song: song, card: true) }
                    }.padding(.horizontal, 16)
                }
            }.padding(.bottom, 40)
        }
        .background(AppTheme.page).navigationTitle(tr("随机选曲"))
        .toolbar { Button(tr("筛选"), systemImage: "line.3.horizontal.decrease.circle") { filters = true } }
        .sheet(isPresented: $filters) { CatalogFilterSheet(store: catalog) }
        .onChange(of: count) { spinTask?.cancel(); spinning = false; songs = []; displayed = [] }
        .onDisappear { spinTask?.cancel(); spinning = false; displayed = songs }
    }

    private func draw() {
        if spinning { spinTask?.cancel(); displayed = songs; spinning = false; return }
        guard let bundle = catalog.bundle else { return }
        let ids = Set(catalog.songs.map(\.id))
        let pool = bundle.catalog.songs.filter { ids.contains($0.songId) }
        songs = personal.bridge.randomSongs(songs: pool, count: count).compactMap { result in catalog.allSongs.first { $0.id == result.songId } }
        guard !reduceMotion else { displayed = songs; return }
        spinning = true
        spinTask = Task {
            // The draw is shared business logic; these changing covers are presentation only.
            for step in 0..<16 {
                guard !Task.isCancelled else { return }
                displayed = Array(catalog.songs.shuffled().prefix(Int(count)))
                do { try await Task.sleep(for: .milliseconds(50 + step * 8)) } catch { return }
            }
            displayed = songs
            spinning = false
        }
    }
}
