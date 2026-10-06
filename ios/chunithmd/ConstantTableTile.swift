import Shared
import SwiftUI

struct ConstantTableTile: View {
    let entry: ConstantTableEntry
    let includesScores: Bool
    let song: CatalogSongViewData?
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        Button {
            if let song { navigation.open(song, sheetID: entry.type + ":" + entry.difficulty, sourceID: identity) }
        } label: { jacket }
        .buttonStyle(.plain).disabled(song == nil)
        .modifier(SongTransitionSource(id: identity))
        .accessibilityIdentifier("constant-tile-" + entry.sheetKey)
        .accessibilityLabel(accessibleTitle)
        .accessibilityValue(accessibleScores)
    }

    private var jacket: some View {
        JacketImage(url: catalog.jacketURL(for: entry.imageName))
            .aspectRatio(1, contentMode: .fit).clipShape(.rect(cornerRadius: 9))
            .overlay { RoundedRectangle(cornerRadius: 9).strokeBorder(difficultyStyle(entry.difficulty, type: entry.type), lineWidth: 1.5) }
            .overlay(alignment: .bottomTrailing) {
                if includesScores { ConstantTableBadges(entry: entry).padding(2) }
            }
    }

    private var accessibleTitle: String {
        [entry.title, entry.difficulty.uppercased(), entry.constant.formatted(.number.precision(.fractionLength(1)))].joined(separator: ", ")
    }

    private var accessibleScores: String {
        guard includesScores else { return "" }
        let labels: [String?] = [entry.rank, entry.fullCombo, entry.fullChain]
        return labels.compactMap { $0 }.map { tr($0) }.joined(separator: ", ")
    }
}
