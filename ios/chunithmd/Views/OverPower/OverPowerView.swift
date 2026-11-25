import Shared
import SwiftUI

struct OverPowerView: View {
    @Environment(PersonalStore.self) private var personal
    @Environment(CatalogStore.self) private var catalog
    @State private var hot = false
    @State private var byLevel = false
    @State private var recorded = true
    @State private var selectedLevel: String?
    @State private var songOrder = OverPowerOrder(field: .remaining, ascending: false)
    @State private var levelOrder = OverPowerOrder(field: .level, ascending: false)

    var body: some View {
        List {
            if let report = personal.overPower {
                let scope = hot ? report.hot : report.all
                if scope.songs.isEmpty {
                    Text(tr("当前范围没有可统计的歌曲")).foregroundStyle(.secondary)
                } else {
                    Section {
                        OverPowerSummaryView(points: scope.points, coverage: tr("已记录歌曲"),
                                             recorded: scope.recordedCount, total: scope.songs.count)
                    }
                    Section {
                        Picker(tr("歌曲明细"), selection: $byLevel) {
                            Text(tr("歌曲明细")).tag(false)
                            Text(tr("按谱面等级")).tag(true)
                        }.pickerStyle(.segmented)
                        if !byLevel {
                            Picker(selection: $recorded) {
                                Text(tr("已记录")).tag(true)
                                Text(tr("未记录")).tag(false)
                            } label: { Text(tr("成绩记录")) }
                            .tint(.secondary)
                            .accessibilityIdentifier("overpower-record-filter")
                        }
                    }
                    Section {
                        if byLevel {
                            ForEach(levelOrder.levels(scope: scope), id: \.level) { level in
                                Button { selectedLevel = level.level } label: {
                                    VStack(alignment: .leading, spacing: 6) {
                                        HStack {
                                            Text("Lv. " + level.level).font(.headline)
                                            Spacer()
                                            Text(level.points.rateText).monospacedDigit()
                                            Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                                        }
                                        ProgressView(value: level.points.percent, total: 100)
                                        Text(tr("已记录谱面 {0} / {1}", level.recordedCount, level.charts.count))
                                            .font(.caption).foregroundStyle(.secondary)
                                    }.padding(.vertical, 4).contentShape(.rect)
                                }.buttonStyle(.plain)
                            }
                        } else {
                            let songs = songOrder.songs(scope: scope, recorded: recorded)
                            if songs.isEmpty { Text(tr("没有符合条件的歌曲")).foregroundStyle(.secondary) }
                            ForEach(songs, id: \.songId) { song in
                                OverPowerScoreRow(chart: song.displayChart, points: song.points, songTotal: true)
                            }
                        }
                    }
                }
            } else if catalog.bundle == nil {
                CatalogLoadingView()
            } else {
                ProgressView(tr("正在加载…"))
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(HomeDestination.overPower.title)
        .navigationBarTitleDisplayMode(.large)
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button(hot ? tr("全曲") : tr("HOT·新曲"), systemImage: "rectangle.2.swap") {
                    hot.toggle()
                }
                .labelStyle(.iconOnly).tint(.primary)
                .accessibilityValue(hot ? tr("HOT·新曲") : tr("全曲"))
                .accessibilityIdentifier("overpower-scope")
                OverPowerSortMenu(order: byLevel ? $levelOrder : $songOrder, levels: byLevel)
            }
        }
        .navigationDestination(item: $selectedLevel) { level in
            OverPowerLevelView(level: level, hot: hot)
        }
    }
}
