import Shared
import SwiftUI

struct ScoreQueryStatsView: View {
    let stats: ScoreQueryStats

    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 0) {
                item(stats.chartCount, label: tr("谱面"))
                Divider().frame(height: 30)
                item(stats.songCount, label: tr("歌曲"))
                Divider().frame(height: 30)
                item(stats.sssPlusCount, label: "SSS+")
                Divider().frame(height: 30)
                item(stats.sssCount, label: "SSS")
            }
            Divider()
            HStack(spacing: 0) {
                item(stats.fcCount, label: "FC")
                Divider().frame(height: 30)
                item(stats.ajCount, label: "AJ")
                Divider().frame(height: 30)
                item(stats.ajcCount, label: "AJC")
                Divider().frame(height: 30)
                item(stats.fullChainCount, label: tr("Full Chain").uppercased())
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("score-query-stats")
    }

    private func item(_ value: Int32, label: String) -> some View {
        VStack(spacing: 4) {
            Text(value.formatted()).font(.system(size: 22, weight: .bold, design: .rounded))
                .contentTransition(.numericText())
            Text(label).font(.system(size: 10, weight: .medium)).foregroundStyle(.secondary)
                .lineLimit(1).minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}
