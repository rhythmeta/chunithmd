import SwiftUI
import Shared

struct BestChartSummaryView: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(PersonalStore.self) private var personal
    var body: some View {
        if let best = personal.best.first(where: { $0.chartId == song.id + ":" + sheet.id }) {
            VStack(alignment: .leading, spacing: 12) {
                HStack { Text(tr("个人最佳")).font(.system(size: 13, weight: .semibold)); Spacer(); Text(best.rank).font(.system(size: 13, weight: .semibold)).foregroundStyle(.orange) }
                HStack(alignment: .firstTextBaseline) {
                    Text(Int(best.score).formatted()).font(.system(size: 28, weight: .black, design: .monospaced))
                    Spacer()
                    Text("R \(best.rating.formatted(.number.precision(.fractionLength(2))))").font(.headline.monospacedDigit()).foregroundStyle(.secondary)
                }
                HStack {
                    if let clear = best.clear { Text(ClearType.companion.displayName(value: clear) ?? clear.uppercased()) }
                    if let combo = best.fullCombo { Text(FullComboType.companion.displayName(value: combo) ?? combo) }
                    if let chain = best.fullChain { Text(tr(FullChainType.companion.displayName(value: chain) ?? chain)) }
                }.font(.caption.bold()).foregroundStyle(.orange)
            }.padding(12).background(.secondary.opacity(0.06), in: .rect(cornerRadius: 12))
        }
    }
}
