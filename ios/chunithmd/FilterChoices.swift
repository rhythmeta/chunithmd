import SwiftUI

struct FilterChoices: View {
    let options: [String]
    @Binding var selection: Set<String>
    var difficulty = false
    var body: some View {
        FilterFlowLayout(spacing: 10) {
            ForEach(options, id: \.self) { option in
                let selected = selection.contains(option)
                let color = difficulty ? difficultyStyle(option) : AnyShapeStyle(.blue)
                Button {
                    if selected { selection.remove(option) } else { selection.insert(option) }
                } label: {
                    Text(difficulty ? option.uppercased() : option)
                        .font(.system(size: 13, weight: .semibold))
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .foregroundStyle(selected ? AnyShapeStyle(.white) : AnyShapeStyle(.primary))
                        .background(selected ? color : AnyShapeStyle(.primary.opacity(0.06)), in: .capsule)
                        .overlay { Capsule().stroke(selected ? AnyShapeStyle(color.opacity(0.3)) : AnyShapeStyle(.primary.opacity(0.08))) }
                }.buttonStyle(.plain).accessibilityAddTraits(selected ? .isSelected : [])
            }
        }
    }
}
