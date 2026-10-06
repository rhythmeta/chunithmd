import Shared
import SwiftUI

struct ScoreHistoryRow: View {
    let record: ScoreRecord
    let isBest: Bool
    let alternate: Bool
    let tint: AnyShapeStyle
    let onDelete: () -> Void

    var body: some View {
        let date = Date(timeIntervalSince1970: Double(record.playedAt) / 1000)
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(date, format: .dateTime.year(.twoDigits).month(.defaultDigits).day(.defaultDigits))
                    .font(.caption.bold())
                Text(date, style: .time).font(.caption).foregroundStyle(.secondary)
            }.frame(width: 70, alignment: .leading)
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 4) {
                    Text(record.rank).font(.caption.bold()).fontDesign(.rounded).foregroundStyle(scoreRankColor(record.rank))
                    Text(Int(record.score).formatted()).font(.caption.monospaced().bold())
                }.lineLimit(1).minimumScaleFactor(0.75)
                ScoreStatusBadges(clear: record.clear, combo: record.fullCombo, chain: record.fullChain, tint: tint)
            }.layoutPriority(1)
            Spacer(minLength: 0)
            Button(action: onDelete) {
                Label(tr("删除成绩"), systemImage: "trash").labelStyle(.iconOnly)
                    .font(.caption).foregroundStyle(.red.opacity(0.6))
            }.buttonStyle(.plain)
        }.padding(.horizontal, 20).padding(.vertical, 8)
            .background {
                if isBest {
                    RoundedRectangle(cornerRadius: 8).fill(tint.opacity(0.1))
                        .overlay { RoundedRectangle(cornerRadius: 8).strokeBorder(tint, lineWidth: 1.5) }
                        .padding(.horizontal, 10).padding(.vertical, 2)
                } else if alternate { Color.primary.opacity(0.02) }
            }
            .accessibilityElement(children: .contain)
            .accessibilityIdentifier("score-history-row-" + record.id)
    }
}
