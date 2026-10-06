import SwiftUI

struct SettingsRowLabel: View {
    let title: String
    let icon: String
    let color: Color
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon).font(.system(size: 16)).foregroundStyle(.white)
                .frame(width: 28, height: 28).background(color, in: .rect(cornerRadius: 6))
            Text(title).foregroundStyle(.primary)
        }
    }
}
