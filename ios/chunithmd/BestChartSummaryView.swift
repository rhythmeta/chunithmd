import SwiftUI
import Shared

struct BestChartSummaryView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(PersonalStore.self) private var personal
    var body: some View {
        if let best = personal.best.first(where: { $0.chartId == song.id + ":" + sheet.id }) {
            VStack(alignment: .leading, spacing: 3) {
                Text(tr("当前最佳")).font(.system(size: 11, weight: .medium)).foregroundStyle(.secondary)
                HStack(alignment: .firstTextBaseline) {
                    Text(Int(best.score).formatted()).font(.system(size: 19, weight: .bold, design: .rounded))
                    Text(best.rank).font(.system(size: 19, weight: .bold, design: .rounded)).foregroundStyle(scoreRankColor(best.rank))
                    Spacer()
                }.lineLimit(1).minimumScaleFactor(0.75)
                ScoreStatusBadges(clear: best.clear, combo: best.fullCombo, chain: best.fullChain,
                                  tint: difficultyStyle(sheet.difficulty, type: sheet.type))
            }.padding(.horizontal, 16)
        } else {
            Text(tr("暂无成绩")).font(.system(size: 13)).foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading).padding(.horizontal, 16)
        }
    }
}
