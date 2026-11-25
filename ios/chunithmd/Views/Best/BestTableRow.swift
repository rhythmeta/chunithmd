import SwiftUI
import Shared

struct BestTableRow: View {
    let entry: BestTableEntry
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @Environment(\.colorScheme) private var colorScheme
    @State private var identity = UUID()

    var body: some View {
        let song = catalog.allSongs.first { $0.id == entry.songId }
        let tint = difficultyStyle(entry.difficulty, type: entry.type)
        Button {
            if let song { navigation.open(song, sheetID: entry.type + ":" + entry.difficulty, sourceID: identity) }
        } label: {
            ScoreEntryRowLabel(title: entry.title, jacket: catalog.jacketURL(for: entry.imageName),
                               tint: tint, rank: entry.rank, score: entry.score) {
                ScoreStatusBadges(clear: entry.clear, combo: entry.fullCombo, chain: entry.fullChain, tint: tint, font: .system(size: 9, weight: .bold))
            } value: {
                Text(entry.rating, format: .number.precision(.fractionLength(2)))
                    .font(.system(size: 18, weight: .black, design: .rounded))
                    .foregroundStyle(ratingTierStyle(entry.rating))
                    .brightness(colorScheme == .light ? -0.2 : 0)
                Text(tr("定数 {0}", entry.constant.formatted(.number.precision(.fractionLength(1)))))
                    .font(.system(size: 11)).foregroundStyle(.secondary)
            }
        }
        .buttonStyle(.plain).disabled(song == nil)
        .accessibilityIdentifier("best-row-" + entry.chartId)
        .modifier(SongTransitionSource(id: identity))
    }
}
