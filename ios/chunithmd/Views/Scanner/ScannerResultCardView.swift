import Shared
import SwiftUI

struct ScannerResultCardView: View {
    let result: ScannerResult
    let song: CatalogSongViewData
    let onTap: () -> Void
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        let color = difficultyStyle(result.match.difficulty, type: result.match.type)
        Button(action: onTap) {
            HStack(spacing: 12) {
                RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 4)
                JacketImage(url: catalog.jacketURL(for: song.imageName))
                    .frame(width: 44, height: 44).clipShape(.rect(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 3) {
                    Text(song.title).font(.subheadline.bold()).foregroundStyle(.primary).lineLimit(2)
                    Text(result.match.type.lowercased() == "we" ? "WE · " + result.match.difficulty : result.match.difficulty.uppercased())
                        .font(.caption.bold()).foregroundStyle(color)
                }
                Spacer(minLength: 4)
                VStack(alignment: .trailing, spacing: 2) {
                    Text(result.score, format: .number.grouping(.never)).monospacedDigit().font(.subheadline.bold())
                    Text(ChunithmScoreRules.shared.rank(score: Int32(result.score)))
                        .font(.caption.bold()).foregroundStyle(color)
                }
                Text(result.match.level).font(.title2.bold()).foregroundStyle(color)
                Image(systemName: "chevron.right").font(.caption.bold()).foregroundStyle(.secondary)
            }
            .padding(.vertical, 14).padding(.horizontal, 16)
            .fixedSize(horizontal: false, vertical: true)
            .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(color.opacity(0.2), lineWidth: 1) }
        }
        .buttonStyle(.plain)
        .accessibilityHint(tr("记录成绩"))
        .accessibilityIdentifier("scanner-result-card")
    }
}
