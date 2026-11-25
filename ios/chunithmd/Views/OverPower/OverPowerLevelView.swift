import Shared
import SwiftUI

struct OverPowerLevelView: View {
    let level: String
    let hot: Bool
    @Environment(PersonalStore.self) private var personal
    @State private var order = OverPowerOrder(field: .name, ascending: true)

    var body: some View {
        List {
            if let report = personal.overPower {
                let scope = hot ? report.hot : report.all
                if let group = scope.levels.first(where: { $0.level == level }) {
                    Section {
                        OverPowerSummaryView(points: group.points, coverage: tr("已记录谱面"),
                                             recorded: group.recordedCount, total: group.charts.count)
                    }
                    Section {
                        ForEach(order.charts(level: group), id: \.sheetKey) { chart in
                            OverPowerScoreRow(chart: chart, points: chart.points)
                        }
                    }
                } else {
                    Text(tr("没有符合条件的谱面")).foregroundStyle(.secondary)
                }
            } else {
                ProgressView(tr("正在加载…"))
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Lv. " + level)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                OverPowerSortMenu(order: $order)
            }
        }
    }
}
