import SwiftUI

struct ScannerSongCardView: View {
    let song: CatalogSongViewData
    let onTap: () -> Void
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                JacketImage(url: catalog.jacketURL(for: song.imageName))
                    .frame(width: 40, height: 40)
                    .clipShape(.rect(cornerRadius: 8))
                    .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text(song.title).font(.subheadline.scaled(by: 14.0 / 15).bold())
                        .foregroundStyle(.primary).lineLimit(1)
                    Text(song.artist).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                }
                Spacer(minLength: 4)
                Image(systemName: "chevron.right").font(.caption.bold())
                    .foregroundStyle(.secondary.opacity(0.4)).accessibilityHidden(true)
            }
            .padding(.vertical, 14).padding(.horizontal, 16)
            .fixedSize(horizontal: false, vertical: true)
            .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(.primary.opacity(0.1), lineWidth: 1) }
        }
        .buttonStyle(.plain)
        .accessibilityHint(tr("查看歌曲"))
        .accessibilityIdentifier("scanner-song-card")
    }
}
