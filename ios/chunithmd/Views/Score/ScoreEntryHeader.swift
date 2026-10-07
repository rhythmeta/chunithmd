import SwiftUI

struct ScoreEntryHeader: View {
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    @ScaledMetric(relativeTo: .title3) private var accentHeight = 50

    var body: some View {
        let tint = difficultyStyle(sheet.difficulty, type: sheet.type)
        HStack(spacing: 16) {
            RoundedRectangle(cornerRadius: 3).fill(tint).frame(width: 5, height: accentHeight)
            VStack(alignment: .leading, spacing: 6) {
                Text(song.title).font(.headline.bold()).foregroundStyle(.primary).lineLimit(2)
                HStack(spacing: 6) {
                    Text(sheet.type.uppercased()).font(.caption.bold()).foregroundStyle(.white)
                        .padding(.horizontal, 6).padding(.vertical, 3)
                        .background(tint, in: .capsule)
                    Text(sheet.difficulty.uppercased()).font(.subheadline.weight(.semibold)).foregroundStyle(tint)
                }
            }
            Spacer(minLength: 4)
            Text("Lv." + sheet.level).font(.title2.bold()).foregroundStyle(tint).multilineTextAlignment(.trailing)
        }
        .fontDesign(.rounded).padding(16)
        .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}
