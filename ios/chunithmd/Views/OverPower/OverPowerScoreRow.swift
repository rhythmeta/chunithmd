import Shared
import SwiftUI

struct OverPowerScoreRow: View {
    let chart: OverPowerChart
    let points: OverPowerPoints
    var songTotal = false
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        let tint = difficultyStyle(chart.difficulty)
        let difficulty = chart.difficulty.uppercased()
        Button {
            guard let song = catalog.allSongs.first(where: { $0.id == chart.songId }) else { return }
            let sheetID = String(chart.sheetKey.dropFirst(chart.songId.count + 1))
            navigation.open(song, sheetID: sheetID, sourceID: identity)
        } label: {
            ScoreEntryRowLabel(title: chart.title, jacket: catalog.jacketURL(for: chart.imageName),
                               tint: tint, rank: chart.best?.rank, score: chart.best?.score) {
                HStack(spacing: 4) {
                    ScoreTintBadge(text: songTotal && chart.recorded ? tr("{0} 计入", difficulty) : difficulty,
                                   tint: tint, font: .caption2.bold())
                    if let combo = FullComboType.companion.displayName(value: chart.best?.fullCombo) {
                        ScoreTintBadge(text: combo, tint: AnyShapeStyle(.orange), font: .caption2.bold())
                    }
                }
            } value: {
                Text(opNumber(points.earned))
                    .font(.system(size: 18, weight: .black, design: .rounded))
                    .foregroundStyle(points.tierStyle)
                Text("/ " + opNumber(points.maximum) + " OP").font(.caption).foregroundStyle(.secondary)
            }
        }
        .buttonStyle(.plain)
        .modifier(SongTransitionSource(id: identity))
    }
}
