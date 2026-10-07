import SwiftUI

struct SongDetailCopyAction: ViewModifier {
    let text: String
    let onCopy: (String) -> Void

    func body(content: Content) -> some View {
        Button { onCopy(text) } label: {
            content.contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityHint(tr("复制"))
        .contextMenu {
            Button(tr("复制"), systemImage: "doc.on.doc") { onCopy(text) }
        }
    }
}
