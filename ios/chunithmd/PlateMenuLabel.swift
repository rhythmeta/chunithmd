import SwiftUI

struct PlateMenuLabel: View {
    let title: String
    let selection: String
    let systemImage: String
    @ScaledMetric(relativeTo: .caption) private var captionHeight = 18

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: systemImage)
                .font(.caption.bold()).foregroundStyle(.secondary)
                .frame(height: captionHeight)
            HStack(spacing: 6) {
                Text(selection).font(.subheadline.bold()).lineLimit(1).minimumScaleFactor(0.8)
                Spacer(minLength: 0)
                Image(systemName: "chevron.down").font(.caption.bold()).foregroundStyle(.tertiary)
            }
        }
        .padding(12).frame(maxWidth: .infinity, minHeight: 54, alignment: .leading)
        .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
    }
}
