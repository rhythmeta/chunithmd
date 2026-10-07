import SwiftUI

struct FilterChoices: View {
    let options: [String]
    @Binding var selection: Set<String>
    var difficulty = false
    var displayValue: (String) -> String = { $0 }
    var tint: (String) -> AnyShapeStyle = { _ in AnyShapeStyle(.blue) }
    var body: some View {
        FilterFlowLayout(spacing: 10) {
            ForEach(options, id: \.self) { option in
                let selected = selection.contains(option)
                let color = difficulty ? difficultyStyle(option) : tint(option)
                let border = selected && difficulty && option.lowercased() == "world's end"
                    ? AnyShapeStyle(.white.opacity(0.72))
                    : selected ? AnyShapeStyle(color.opacity(0.3)) : AnyShapeStyle(.primary.opacity(0.08))
                Button {
                    if selected { selection.remove(option) } else { selection.insert(option) }
                } label: {
                    Text(difficulty ? option.uppercased() : displayValue(option))
                        .font(.system(size: 13, weight: .semibold))
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .foregroundStyle(selected ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
                        .background(selected ? color : AnyShapeStyle(.primary.opacity(0.06)), in: .capsule)
                        .overlay { Capsule().strokeBorder(border) }
                }.buttonStyle(.plain).accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }
}
