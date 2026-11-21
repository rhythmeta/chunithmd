import SwiftUI
import Shared

struct BestTableSummaryView: View {
    let summary: PlayerRatingSummary
    let bestEntries: [BestTableEntry]
    let newEntries: [BestTableEntry]
    @Environment(\.colorScheme) private var scheme

    private let bestAccent = Color(red: 1, green: 0.584, blue: 0.0)
    private let newAccent = Color(red: 0.243, green: 0.608, blue: 1)

    private var bestRating: Double { bestEntries.reduce(0) { $0 + $1.rating } }
    private var newRating: Double { newEntries.reduce(0) { $0 + $1.rating } }
    private var totalRating: Double { bestRating + newRating }
    private var entryCount: Int { bestEntries.count + newEntries.count }
    private var averageRating: Double { entryCount > 0 ? totalRating / Double(entryCount) : 0 }
    private var newPercent: Double { totalRating > 0 ? newRating * 100 / totalRating : 0 }
    private var bestPercent: Double { totalRating > 0 ? bestRating * 100 / totalRating : 0 }

    var body: some View {
        VStack(spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(tr("玩家 Rating")).font(.footnote).foregroundStyle(.secondary)
                    Text(summary.rating, format: .number.precision(.fractionLength(2)))
                        .font(.system(size: 30, weight: .black, design: .rounded))
                        .foregroundStyle(LinearGradient(colors: playerRatingColors(summary.rating), startPoint: .leading, endPoint: .trailing))
                        .brightness(scheme == .light ? -0.18 : 0)
                        .accessibilityIdentifier("best-rating")
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 3) {
                    Text("B\(summary.bestSlotCount)  \(summary.best30Average, format: .number.precision(.fractionLength(2)))")
                    Text("N\(summary.newSlotCount)  \(summary.new20Average, format: .number.precision(.fractionLength(2)))")
                    Text(tr("平均 {0}", averageRating.formatted(.number.precision(.fractionLength(2)))))
                }
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
            }

            VStack(spacing: 6) {
                GeometryReader { proxy in
                    HStack(spacing: 0) {
                        if newRating > 0 { Rectangle().fill(newAccent).frame(width: proxy.size.width * newPercent / 100) }
                        if bestRating > 0 { Rectangle().fill(bestAccent) }
                    }
                }
                .frame(height: 6)
                .background(Color.primary.opacity(0.08), in: .capsule)
                .clipShape(.capsule)
                HStack {
                    Text(tr("N{0} 占比 {1}%", newEntries.count, newPercent.formatted(.number.precision(.fractionLength(1)))))
                        .foregroundStyle(newAccent)
                    Spacer()
                    Text(tr("B{0} 占比 {1}%", bestEntries.count, bestPercent.formatted(.number.precision(.fractionLength(1)))))
                        .foregroundStyle(bestAccent)
                }
                .font(.caption2.bold())
            }

            VStack(spacing: 6) {
                HStack {
                    Text(tr("平均分数")).frame(maxWidth: .infinity, alignment: .trailing)
                }
                .font(.caption2.bold()).foregroundStyle(.secondary).padding(.horizontal, 8)
                statsRow(label: "B\(entryCount)", entries: bestEntries + newEntries, background: Color.primary.opacity(0.05))
                statsRow(label: "B\(bestEntries.count)", entries: bestEntries, background: bestAccent.opacity(0.12))
                statsRow(label: "N\(newEntries.count)", entries: newEntries, background: newAccent.opacity(0.12))
            }
        }
        .padding(18)
        .background(Color(.secondarySystemGroupedBackground), in: .rect(cornerRadius: 18))
    }

    private func statsRow(label: String, entries: [BestTableEntry], background: Color) -> some View {
        HStack {
            Text(label).frame(width: 48, alignment: .leading)
            Spacer()
            Text(averageScore(entries)?.formatted() ?? "-")
                .font(.caption.monospacedDigit())
        }
        .font(.caption).foregroundStyle(.secondary)
        .padding(.horizontal, 8).padding(.vertical, 5)
        .background(background, in: .rect(cornerRadius: 8))
    }

    private func averageScore(_ entries: [BestTableEntry]) -> Int? {
        guard !entries.isEmpty else { return nil }
        return Int(entries.reduce(Int64(0)) { $0 + Int64($1.score) } / Int64(entries.count))
    }
}
