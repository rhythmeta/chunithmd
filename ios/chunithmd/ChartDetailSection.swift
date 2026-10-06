import SwiftUI
import Shared

struct ChartDetailSection: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @Environment(PersonalStore.self) private var personal
    @State private var target = 1_009_000
    @State private var showRating = false
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text(tr("谱面信息")).font(.system(size: 13, weight: .semibold))
                Spacer()
                Text(sheet.difficulty.uppercased()).font(.caption.bold()).foregroundStyle(difficultyStyle(sheet.difficulty))
            }
            LabeledContent(tr("定数"), value: (sheet.internalLevelValue ?? sheet.levelValue).map { $0.formatted(.number.precision(.fractionLength(1))) } ?? "—")
            LabeledContent(tr("谱师"), value: sheet.noteDesigner ?? "—")
            if let notes = sheet.noteCounts {
                HStack {
                    ForEach(Array(zip(["TAP", "HOLD", "SLIDE", "AIR", "FLICK"], [notes.tap, notes.hold, notes.slide, notes.air, notes.flick])), id: \.0) { name, count in
                        VStack(spacing: 5) { Text(name).font(.caption); Text(count.map(String.init) ?? "—").font(.subheadline.bold()) }.frame(maxWidth: .infinity)
                    }
                }.padding(.vertical, 8)
                if let total = notes.total {
                    LabeledContent(tr("总物量"), value: total.formatted())
                    Picker(tr("目标分数"), selection: $target) {
                        Text("SSS+").tag(1_009_000); Text("SSS").tag(1_007_500); Text("SS+").tag(1_005_000); Text("SS").tag(1_000_000)
                    }.pickerStyle(.segmented)
                    if let tolerance = ScoreToleranceCalculator.shared.calculate(totalNotes: KotlinInt(int: Int32(total)), targetScore: Int32(target)) {
                        LabeledContent(tr("容错"), value: "J \(tolerance.justice) · A \(tolerance.attack) · M \(tolerance.miss)")
                        Text(tr("各项独立计算，其余音符均为 JUSTICE CRITICAL。")).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
            DisclosureGroup(tr("Rating 对照表"), isExpanded: $showRating) {
                ForEach(personal.bridge.ratingTable(constant: sheet.internalLevelValue ?? sheet.levelValue ?? 0), id: \.rank) { row in
                    HStack { Text(row.rank); Spacer(); Text(Int(row.score).formatted()); Text(row.rating, format: .number.precision(.fractionLength(2))).frame(width: 65, alignment: .trailing) }
                        .font(.subheadline.monospacedDigit()).padding(.vertical, 4)
                }
            }
        }.font(.system(size: 12))
    }
}
