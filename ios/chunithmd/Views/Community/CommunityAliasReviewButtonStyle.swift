import SwiftUI

struct CommunityAliasReviewButtonStyle: PrimitiveButtonStyle {
    let approve: Bool
    let alias: String
    let moderate: () -> Void
    @Environment(\.isEnabled) private var isEnabled
    @State private var confirming = false

    func makeBody(configuration: Configuration) -> some View {
        let action = approve ? tr("设为通过") : tr("设为拒绝")
        configuration.label
            .gesture(
                LongPressGesture().exclusively(before: TapGesture()).onEnded { gesture in
                    guard isEnabled else { return }
                    switch gesture {
                    case .first: confirming = true
                    case .second: configuration.trigger()
                    }
                }
            )
            .accessibilityAddTraits(.isButton)
            .accessibilityAction { if isEnabled { configuration.trigger() } }
            .accessibilityAction(named: Text(action)) { if isEnabled { confirming = true } }
            .sensoryFeedback(.impact(weight: .light), trigger: confirming) { _, presented in presented }
            .alert(approve ? tr("直接通过这个别名？") : tr("直接拒绝这个别名？"), isPresented: $confirming) {
                Button(action, role: approve ? nil : .destructive) { if isEnabled { moderate() } }
                Button(tr("取消"), role: .cancel) {}
            } message: {
                Text(verbatim: alias)
            }
    }
}
