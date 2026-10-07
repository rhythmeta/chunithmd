import Shared
import SwiftUI

struct ScoreEntryBestCard: View {
    let best: BestScoreSummary
    let date: Date?
    let tint: AnyShapeStyle

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "clock.arrow.circlepath").font(.subheadline).foregroundStyle(.secondary).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text(tr("当前最佳")).font(.footnote).foregroundStyle(.secondary)
                Text(Int(best.score).formatted() + " · " + best.rank).font(.headline.bold()).monospacedDigit()
                ScoreStatusBadges(clear: best.clear, combo: best.fullCombo, chain: best.fullChain, tint: tint)
                if let date { Text(date, style: .date).font(.footnote).foregroundStyle(.secondary) }
            }
            Spacer(minLength: 0)
        }
        .padding(14).frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.primary.opacity(0.03), in: .rect(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}
