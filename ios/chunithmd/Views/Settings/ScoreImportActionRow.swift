import SwiftUI

struct ScoreImportIcon: View {
    let symbol: String
    let tint: Color
    @ScaledMetric(relativeTo: .body) private var size = 32

    var body: some View {
        Image(systemName: symbol)
            .font(.subheadline.bold())
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .background(tint.gradient, in: .rect(cornerRadius: 10))
            .accessibilityHidden(true)
    }
}

struct ScoreImportActionRow: View {
    let title: String
    let symbol: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                ScoreImportIcon(symbol: symbol, tint: tint)
                Text(title).foregroundStyle(.primary)
                Spacer()
                Image(systemName: "arrow.up.forward.app")
                    .font(.subheadline.bold())
                    .foregroundStyle(tint)
                    .accessibilityHidden(true)
            }
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}
