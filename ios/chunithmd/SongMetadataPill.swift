import SwiftUI

struct SongMetadataPill: View {
    let icon: String
    let value: String
    var label: String? = nil
    var fillsWidth = false
    var body: some View {
        HStack(spacing: 5) {
            Image(systemName: icon).font(.system(size: 10, weight: .semibold)).foregroundStyle(.secondary)
            HStack(spacing: 2) {
                Text(value).font(.system(size: 12, weight: .semibold, design: .rounded)).lineLimit(1)
                if let label { Text(label).font(.system(size: 10, weight: .medium)).foregroundStyle(.secondary) }
            }
        }
        .frame(maxWidth: fillsWidth ? .infinity : nil)
        .padding(.horizontal, 12).padding(.vertical, 8)
        .background(.ultraThinMaterial, in: .capsule)
        .overlay { Capsule().strokeBorder(.primary.opacity(0.06)) }
    }
}
