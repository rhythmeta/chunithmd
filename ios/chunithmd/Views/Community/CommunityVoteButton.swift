import SwiftUI

struct CommunityVoteButton: View {
    let support: Bool
    let count: Int
    let selected: Bool
    let inFlight: Bool
    let alias: String
    let moderate: (() -> Void)?
    let action: () -> Void
    private var tint: Color { support ? .green : .red }
    private var title: String {
        support ? (selected ? tr("取消支持") : tr("支持")) : (selected ? tr("取消反对") : tr("反对"))
    }

    var body: some View {
        let button = Button(action: action) {
            HStack(spacing: 6) {
                if inFlight { ProgressView().controlSize(.mini) }
                Image(systemName: (support ? "hand.thumbsup" : "hand.thumbsdown") + (selected ? ".fill" : ""))
                Text("\(title) \(count)")
            }
            .font(.system(size: 12, weight: .semibold))
            .padding(.horizontal, 12).padding(.vertical, 6)
            .foregroundStyle(selected ? .white : tint)
            .background(selected ? tint : .clear, in: .capsule)
            .overlay { Capsule().strokeBorder(tint.opacity(0.35), lineWidth: 1) }
            .frame(minHeight: 44).contentShape(.rect)
        }
        .accessibilityAddTraits(selected ? .isSelected : [])

        if let moderate {
            button.buttonStyle(CommunityAliasReviewButtonStyle(approve: support, alias: alias, moderate: moderate))
        } else {
            button.buttonStyle(.plain)
        }
    }
}
