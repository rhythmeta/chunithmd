import Shared
import SwiftUI

struct ScoreQueryEntryView: View {
    let entry: ScoreQueryEntry
    let song: CatalogSongViewData?
    var grid = false
    var cornerRadius = 6.0
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        Button {
            if let song { navigation.open(song, sheetID: entry.type + ":" + entry.difficulty, sourceID: identity) }
        } label: {
            if grid { gridCell } else { listRow }
        }
        .buttonStyle(.plain).disabled(song == nil)
        .modifier(SongTransitionSource(id: identity))
        .accessibilityIdentifier((grid ? "score-query-tile-" : "score-query-row-") + entry.sheetKey)
        .accessibilityLabel("\(entry.title), \(entry.difficulty.uppercased()), \(entry.score.formatted()), \(entry.rank), \(combo ?? ""), \(chain ?? ""), \(entry.rating.formatted(.number.precision(.fractionLength(2))))")
    }

    private var tint: AnyShapeStyle { difficultyStyle(entry.difficulty, type: entry.type) }
    private var combo: String? { ScoreQueryCalculatorKt.displayFullCombo(value: entry.fullCombo).map { tr($0) } }
    private var chain: String? { ScoreQueryCalculatorKt.displayFullChain(value: entry.fullChain).map { tr($0) } }
    private var comboColor: Color { scoreQueryComboColor(entry.fullCombo) }
    private var chainColor: Color { scoreQueryChainColor(entry.fullChain) }

    private var gridCell: some View {
        JacketImage(url: catalog.jacketURL(for: entry.imageName))
            .aspectRatio(1, contentMode: .fit)
            .clipShape(.rect(cornerRadius: cornerRadius))
            .overlay { RoundedRectangle(cornerRadius: cornerRadius).strokeBorder(tint, lineWidth: 2) }
            .overlay(alignment: .bottomTrailing) {
                VStack(alignment: .trailing, spacing: 2) {
                    badge(entry.rank, color: scoreRankColor(entry.rank))
                    if let combo { badge(combo, color: comboColor) }
                    if let chain { badge(chain, color: chainColor) }
                }.padding(2)
            }
    }

    private var listRow: some View {
        HStack(spacing: 10) {
            RoundedRectangle(cornerRadius: 2).fill(tint).frame(width: 3).padding(.vertical, 6)
            JacketImage(url: catalog.jacketURL(for: entry.imageName))
                .frame(width: 42, height: 42).clipShape(.rect(cornerRadius: 8))
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.title).font(.system(size: 14, weight: .semibold)).lineLimit(1).foregroundStyle(.primary)
                HStack(spacing: 4) {
                    Text(entry.score.formatted()).font(.system(size: 11, weight: .medium, design: .monospaced))
                        .foregroundStyle(.secondary)
                    if let combo { Text(combo).font(.system(size: 9, weight: .bold)).foregroundStyle(comboColor) }
                    if let chain { Text(chain).font(.system(size: 9, weight: .bold)).foregroundStyle(chainColor) }
                }.lineLimit(1)
            }.frame(maxWidth: .infinity, alignment: .leading)
            VStack(alignment: .trailing, spacing: 2) {
                Text(entry.rank).font(.system(size: 13, weight: .black, design: .rounded))
                    .foregroundStyle(scoreRankColor(entry.rank))
                Text(entry.rating.formatted(.number.precision(.fractionLength(2))))
                    .font(.system(size: 11, weight: .medium, design: .monospaced)).foregroundStyle(.secondary)
            }.fixedSize()
        }
        .padding(.vertical, 8).padding(.horizontal, 10)
        .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 10))
    }

    private func badge(_ text: String, color: Color) -> some View {
        Text(text).font(.system(size: 9, weight: .black, design: .rounded)).foregroundStyle(.white)
            .padding(.horizontal, 3).padding(.vertical, 1)
            .background(color, in: .rect(cornerRadius: 3)).lineLimit(1)
    }
}
