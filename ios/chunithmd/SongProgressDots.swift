import Shared
import SwiftUI

struct SongProgressDots: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    let song: CatalogSongViewData

    private var sheets: [CatalogSongViewData.Sheet] {
        guard let bundle = catalog.bundle else { return [] }
        let ids = NativeCatalogQuery.shared.progressSheetIds(bundle: bundle, songId: song.id, server: catalog.server)
        return ids.compactMap { id in song.sheets.first { $0.id == id } }
    }

    var body: some View {
        HStack(spacing: 3) {
            ForEach(sheets) { sheet in
                let style = sheet.type.lowercased() == "we" ? AnyShapeStyle(WorldsEndStyle.ring) : difficultyStyle(sheet.difficulty)
                let progress = personal.chartProgress[song.id + ":" + sheet.id] ?? 0
                Circle().stroke(style.opacity(0.3), lineWidth: 1.2)
                    .overlay {
                        Circle().trim(from: 0, to: progress).stroke(style, style: StrokeStyle(lineWidth: 4, lineCap: .butt))
                            .frame(width: 4, height: 4).rotationEffect(.degrees(-90))
                    }
                    .frame(width: 8, height: 8)
            }
        }.accessibilityHidden(true)
    }
}
