import SwiftUI
import Shared

struct BestChartSummaryView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(PersonalStore.self) private var personal

    var body: some View {
        let chartID = song.id + ":" + sheet.id
        let op = personal.overPower?.song(songId: song.id)?.charts.first { $0.sheetKey == chartID }?.points
        HStack {
            if let best = personal.best.first(where: { $0.chartId == chartID }) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(tr("当前最佳")).font(.system(size: 11, weight: .medium)).foregroundStyle(.secondary)
                    HStack(alignment: .firstTextBaseline) {
                        Text(Int(best.score).formatted()).font(.system(size: 19, weight: .bold, design: .rounded))
                        Text(best.rank).font(.system(size: 19, weight: .bold, design: .rounded)).foregroundStyle(scoreRankColor(best.rank))
                    }.lineLimit(1).minimumScaleFactor(0.75)
                    ScoreStatusBadges(clear: best.clear, combo: best.fullCombo, chain: best.fullChain,
                                      tint: difficultyStyle(sheet.difficulty, type: sheet.type))
                }
            } else {
                Text(tr("暂无成绩")).font(.system(size: 13)).foregroundStyle(.secondary)
            }
            Spacer(minLength: 6)
            if let op {
                Text(opNumber(op.earned) + " / " + opNumber(op.maximum) + " OP")
                    .font(.subheadline.monospacedDigit()).multilineTextAlignment(.trailing)
            }
        }.padding(.horizontal, 16)
    }
}
