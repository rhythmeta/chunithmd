import Shared
import SwiftUI

struct SongProgressDots: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(PersonalStore.self) private var personal
    let song: CatalogSongViewData
    var preferredSheet: String? = nil

    private var sheets: [CatalogSongViewData.Sheet] {
        if let preferredSheet { return song.sheets.filter { $0.id == preferredSheet } }
        guard let bundle = catalog.bundle else { return [] }
        let ids = NativeCatalogQuery.shared.progressSheetIds(bundle: bundle, songId: song.id, server: catalog.server)
        return ids.compactMap { id in song.sheets.first { $0.id == id } }
    }

    var body: some View {
        HStack(spacing: 3) {
            ForEach(sheets) { sheet in
                let style = sheet.type.lowercased() == "we" ? AnyShapeStyle(WorldsEndStyle.ring) : difficultyStyle(sheet.difficulty)
                let progress = personal.chartProgress[song.id + ":" + sheet.id]
                // The hollow ring fills from 975,000 to 1,000,000, then the inner dot fills to 1,010,000.
                let ring = Double(progress?.ring ?? 0)
                let dot = Double(progress?.dot ?? 0)
                ZStack {
                    Circle().stroke(style.opacity(0.3), lineWidth: 1.2)
                        .frame(width: 8, height: 8)
                    if ring > 0 {
                        Circle().trim(from: 0, to: min(max(ring, 0), 1))
                            .stroke(style, style: StrokeStyle(lineWidth: 1.2, lineCap: .butt))
                            .frame(width: 8, height: 8)
                            .rotationEffect(.degrees(-90))
                    }
                    if dot > 0 {
                        Circle().trim(from: 0, to: min(max(dot, 0), 1))
                            .stroke(style, style: StrokeStyle(lineWidth: 4, lineCap: .butt))
                            .frame(width: 4, height: 4)
                            .rotationEffect(.degrees(-90))
                    }
                }
            }
        }.accessibilityHidden(true)
    }
}
