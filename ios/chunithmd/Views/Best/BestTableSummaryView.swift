import SwiftUI
import Shared

struct BestTableSummaryView: View {
    let response: BestTableResponse
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(tr("玩家 Rating")).font(.subheadline).foregroundStyle(.secondary)
                Text(response.summary.rating, format: .number.precision(.fractionLength(2)))
                    .font(.system(size: 34, weight: .black, design: .rounded))
                    .foregroundStyle(LinearGradient(colors: playerRatingColors(response.summary.rating), startPoint: .leading, endPoint: .trailing))
                    .brightness(scheme == .light ? -0.18 : 0)
                    .accessibilityIdentifier("best-rating")
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text("B\(response.preferences.bestCount) · \(response.summary.best30Average.formatted(.number.precision(.fractionLength(2))))")
                Text("N\(response.preferences.newCount) · \(response.summary.new20Average.formatted(.number.precision(.fractionLength(2))))")
            }.font(.caption.monospacedDigit()).foregroundStyle(.secondary)
        }.padding(.vertical, 8)
    }
}
