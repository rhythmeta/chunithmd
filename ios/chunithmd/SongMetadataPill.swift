import SwiftUI

struct SongMetadataPill: View {
    let icon: String
    let value: String
    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: icon).foregroundStyle(.secondary)
            Text(value).lineLimit(1).minimumScaleFactor(0.8)
        }
        .font(.system(size: 12, weight: .medium))
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 10).padding(.vertical, 10)
        .background(.ultraThinMaterial, in: .capsule)
    }
}
