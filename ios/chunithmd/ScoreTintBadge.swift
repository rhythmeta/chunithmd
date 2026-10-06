import SwiftUI

struct ScoreTintBadge: View {
    let text: String
    let tint: AnyShapeStyle

    var body: some View {
        Text(text).font(.caption.bold()).foregroundStyle(tint)
            .padding(.horizontal, 5).padding(.vertical, 2)
            .background(tint.opacity(0.14), in: .rect(cornerRadius: 5))
            .fixedSize()
    }
}
