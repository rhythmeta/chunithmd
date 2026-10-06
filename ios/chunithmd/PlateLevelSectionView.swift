import Shared
import SwiftUI

struct PlateLevelSectionView: View {
    let section: PlateLevelSection
    let tint: Color
    let remainingOnly: Bool
    let songs: [String: CatalogSongViewData]

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Lv. \(section.level)").font(.headline.bold())
            HStack(spacing: 10) {
                Text(tr("{0} / {1} 已完成", section.completedCount, section.charts.count))
                    .font(.subheadline).foregroundStyle(.secondary)
                ProgressView(value: Double(section.progress)).tint(tint)
            }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 5), spacing: 12) {
                ForEach(section.visibleCharts(remainingOnly: remainingOnly), id: \.sheetKey) { entry in
                    PlateChartTile(entry: entry, tint: tint, song: songs[entry.song.songId])
                }
            }
        }
    }
}
