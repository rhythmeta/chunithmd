import Shared
import SwiftUI

struct ScannerResultCardView: View {
    let result: ScannerResult
    let song: CatalogSongViewData
    let onTap: () -> Void
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        let color = difficultyStyle(result.match.difficulty, type: result.match.type)
        let isWE = result.match.type.lowercased() == "we"
        Button(action: onTap) {
            HStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 4).padding(.vertical, 4)
                HStack(spacing: 12) {
                    JacketImage(url: catalog.jacketURL(for: song.imageName))
                        .frame(width: 40, height: 40)
                        .clipShape(.rect(cornerRadius: 8))
                        .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 3) {
                        HStack(spacing: 4) {
                            Text(result.match.type.uppercased())
                                .font(.caption2.scaled(by: 8.0 / 11).weight(.black))
                                .padding(.horizontal, 4).padding(.vertical, 1)
                                .background(color, in: .rect(cornerRadius: 3)).foregroundStyle(.white)
                            Text(song.title).font(.caption.bold()).foregroundStyle(.primary).lineLimit(1)
                        }
                        Text(isWE ? "WORLD'S END" : result.match.difficulty.uppercased())
                            .font(.footnote.bold()).fontDesign(.rounded).foregroundStyle(color)
                    }
                    Spacer(minLength: 0)
                    VStack(alignment: .trailing, spacing: 1) {
                        Text(result.score, format: .number.grouping(.never))
                            .font(.caption.bold().monospaced()).foregroundStyle(.primary)
                        Text(ChunithmScoreRules.shared.rank(score: Int32(result.score)))
                            .font(.caption2.scaled(by: 10.0 / 11).weight(.black)).fontDesign(.rounded).foregroundStyle(color)
                    }
                    .fixedSize(horizontal: true, vertical: false)
                    Text(result.match.level).font(.title.weight(.black)).fontDesign(.rounded)
                        .foregroundStyle(color.opacity(0.85)).frame(minWidth: 44)
                        .fixedSize(horizontal: true, vertical: false)
                    Image(systemName: "chevron.right").font(.caption.bold())
                        .foregroundStyle(.secondary.opacity(0.4)).accessibilityHidden(true)
                }
                .padding(.leading, 12).padding(.trailing, 16)
            }
            .padding(.vertical, 14)
            .fixedSize(horizontal: false, vertical: true)
            .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(color.opacity(0.2), lineWidth: 1) }
        }
        .buttonStyle(.plain)
        .accessibilityHint(tr("记录成绩"))
        .accessibilityIdentifier("scanner-result-card")
    }
}
