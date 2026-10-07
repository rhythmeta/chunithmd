import Shared
import SwiftUI

struct ScoreEntryInputCard: View {
    @Binding var scoreText: String
    @Binding var clear: String
    @Binding var combo: String
    @Binding var chain: String
    let rank: String
    let validation: String?
    let focus: FocusState<Bool>.Binding
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var scoreFontSize = 48

    private var row: AnyLayout {
        dynamicTypeSize.isAccessibilitySize ? AnyLayout(VStackLayout(spacing: 12)) : AnyLayout(HStackLayout(spacing: 12))
    }

    var body: some View {
        VStack(spacing: 20) {
            VStack(spacing: 12) {
                Text(tr("分数")).font(.subheadline).foregroundStyle(.secondary)
                // Keep raw text so malformed pasted input cannot silently retain an earlier valid score.
                TextField("0", text: $scoreText)
                    .font(.system(size: scoreFontSize, weight: .black, design: .rounded)).monospacedDigit()
                    .foregroundStyle(.primary).multilineTextAlignment(.center)
                    .keyboardType(.numberPad).submitLabel(.done).focused(focus)
                    .minimumScaleFactor(0.5).accessibilityLabel(tr("分数"))
                    .accessibilityIdentifier("score-entry-score")
            }
            row {
                Label(rank, systemImage: "trophy.fill")
                    .font(.title3.bold()).fontDesign(.rounded).monospacedDigit()
                    .foregroundStyle(scoreRankColor(rank))
                    .frame(maxWidth: .infinity).frame(minHeight: 44).padding(.horizontal, 12)
                    .background(scoreRankColor(rank).opacity(rank == "—" ? 0.08 : 0.12), in: .capsule)
                    .accessibilityLabel(tr("评价")).accessibilityValue(rank)
                ScoreEntryStatusMenu(title: tr("CLEAR 状态"), symbol: "flag.checkered", tint: .orange,
                    options: ClearType.entries.reversed().map { ($0.wireValue, $0.displayName) }, selection: $clear)
            }
            if let validation {
                Label(validation, systemImage: "exclamationmark.circle.fill")
                    .font(.footnote).foregroundStyle(.red).frame(maxWidth: .infinity, alignment: .leading)
            }
            row {
                ScoreEntryStatusMenu(title: tr("COMBO 状态"), symbol: "target", tint: .green,
                    options: FullComboType.entries.reversed().map { ($0.wireValue, $0.displayName) }, selection: $combo)
                ScoreEntryStatusMenu(title: tr("CHAIN 状态"), symbol: "person.2.fill", tint: .blue,
                    options: FullChainType.entries.reversed().map { ($0.wireValue, tr($0.displayName)) }, selection: $chain)
            }
        }
        .padding(24).background(.ultraThinMaterial, in: .rect(cornerRadius: 20))
    }
}
