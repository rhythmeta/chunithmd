import SwiftUI

struct BestCapacityFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration.keyboardType(.numberPad).multilineTextAlignment(.center)
            .font(.system(.body, design: .monospaced).bold())
            .frame(width: 60).padding(.vertical, 8)
            .background(.secondary.opacity(0.1), in: .rect(cornerRadius: 8))
            .frame(maxWidth: .infinity)
    }
}
