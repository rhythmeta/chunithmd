import SwiftUI

struct ScoreEntryStatusMenu: View {
    let title: String
    let symbol: String
    let tint: Color
    let options: [(value: String, title: String)]
    @Binding var selection: String

    private var selectedTitle: String { options.first { $0.value == selection }?.title ?? title }

    var body: some View {
        Menu {
            Picker(title, selection: $selection) {
                Text(tr("无")).tag("")
                ForEach(options, id: \.value) { option in Text(option.title).tag(option.value) }
            }
        } label: {
            Label(selectedTitle, systemImage: symbol)
                .font(.subheadline.bold()).lineLimit(1).minimumScaleFactor(0.75)
                .frame(maxWidth: .infinity).frame(minHeight: 44).padding(.horizontal, 12)
                .background(selection.isEmpty ? Color.secondary.opacity(0.08) : tint.opacity(0.12), in: .capsule)
                .foregroundStyle(selection.isEmpty ? .secondary : tint)
        }
        .accessibilityLabel(title).accessibilityValue(selection.isEmpty ? tr("无") : selectedTitle)
    }
}
