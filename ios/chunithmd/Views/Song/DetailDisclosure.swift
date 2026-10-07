import SwiftUI

struct DetailDisclosure<Content: View>: View {
    let title: String
    var trailingValue: String? = nil
    @ViewBuilder let content: Content
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var expanded = false

    var body: some View {
        VStack(spacing: 0) {
            Button {
                withAnimation(reduceMotion ? nil : .spring(duration: 0.35, bounce: 0.2)) { expanded.toggle() }
            } label: {
                HStack {
                    Text(title).font(.system(size: 12, weight: .bold)).foregroundStyle(.primary)
                    Spacer()
                    if let trailingValue {
                        Text(trailingValue).font(.subheadline.monospacedDigit()).foregroundStyle(.secondary)
                    }
                    Image(systemName: "chevron.right").font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.secondary.opacity(0.4)).rotationEffect(.degrees(expanded ? 90 : 0))
                }
                .contentShape(.rect)
                .padding(.horizontal, 20).padding(.vertical, 4)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier(title)
            .accessibilityValue(expanded ? tr("已展开") : tr("已收起"))
            if expanded {
                content.padding(.top, 8).transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }
}
