import SwiftUI
import Shared

struct BestTableRow: View {
    let entry: BestTableEntry
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        let song = catalog.allSongs.first { $0.id == entry.songId }
        let tint = difficultyStyle(entry.difficulty, type: entry.type)
        Button {
            if let song { navigation.open(song, sheetID: entry.type + ":" + entry.difficulty, sourceID: identity) }
        } label: {
            HStack(spacing: 14) {
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 2).fill(tint).frame(width: 4).padding(.vertical, 8)
                    JacketImage(url: catalog.jacketURL(for: entry.imageName))
                        .frame(width: 56, height: 56).clipShape(.rect(cornerRadius: 10))
                }
                VStack(alignment: .leading, spacing: 4) {
                    SongDetailText(text: entry.title, font: .system(size: 15, weight: .bold), lineHeight: 20, alignment: .leading)
                    HStack(spacing: 6) {
                        Text(entry.rank).font(.system(size: 13, weight: .black, design: .rounded))
                            .foregroundStyle(scoreRankColor(entry.rank))
                        Text(Int(entry.score).formatted()).font(.system(size: 12, design: .monospaced)).foregroundStyle(.secondary)
                    }.fixedSize(horizontal: true, vertical: false)
                    ScoreStatusBadges(clear: entry.clear, combo: entry.fullCombo, chain: entry.fullChain, tint: tint, font: .system(size: 9, weight: .bold))
                }.frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
                VStack(alignment: .trailing, spacing: 2) {
                    Text(entry.rating, format: .number.precision(.fractionLength(2)))
                        .font(.system(size: 18, weight: .black, design: .rounded)).foregroundStyle(.orange)
                    Text(tr("定数 {0}", entry.constant.formatted(.number.precision(.fractionLength(1)))))
                        .font(.system(size: 11)).foregroundStyle(.secondary)
                }.fixedSize()
                Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
            }.padding(.vertical, 6).contentShape(.rect)
        }
        .buttonStyle(.plain).disabled(song == nil)
        .accessibilityIdentifier("best-row-" + entry.chartId)
        .modifier(SongTransitionSource(id: identity))
    }
}
