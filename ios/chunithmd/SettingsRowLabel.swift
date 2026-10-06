import SwiftUI

struct SettingsRowLabel: View {
    let title: String
    let icon: String
    let color: Color
    @ScaledMetric(relativeTo: .body) private var titleSize = 16
    @ScaledMetric(relativeTo: .body) private var iconSize = 14
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon).font(.system(size: iconSize)).foregroundStyle(.white)
                .frame(width: 28, height: 28).background(color, in: .rect(cornerRadius: 6))
                .accessibilityHidden(true)
            Text(title).font(.system(size: titleSize)).foregroundStyle(.primary)
        }
    }
}
