import Shared
import SwiftUI

/// Renders the exported Best table to a PNG file.
///
/// The artwork matches the Android client (`BestTableImageExporter`): a 1180pt canvas rendered at
/// 2x with a player header, one grid per section ("N20" / "B30") and a version footer. The caller
/// hands the returned file straight to the system share sheet; there is no in-app preview step.
enum BestTableImageExporter {
    @MainActor
    static func render(
        catalog: CatalogStore,
        userName: String,
        rating: Double,
        bestEntries: [BestTableEntry],
        newEntries: [BestTableEntry],
        bestAverage: Double,
        newAverage: Double,
        bestCount: Int,
        newCount: Int,
        version: String,
        colorScheme: ColorScheme
    ) async throws -> URL {
        var jackets: [String: Image] = [:]
        for name in Set((newEntries + bestEntries).map(\.imageName)) {
            try Task.checkCancellation()
            jackets[name] = await jacket(name, catalog: catalog)
        }
        let poster = BestTablePosterView(
            userName: userName,
            ratingLabel: tr("玩家 Rating"),
            rating: rating,
            summaries: [
                BestTablePosterSummary(id: "N", title: "N\(newCount)", average: newAverage, accent: BestTablePosterLayout.newAccent),
                BestTablePosterSummary(id: "B", title: "B\(bestCount)", average: bestAverage, accent: BestTablePosterLayout.bestAccent),
            ],
            sections: [
                BestTablePosterSection(
                    id: "N", title: "N\(newCount)", average: newAverage, accent: BestTablePosterLayout.newAccent,
                    entries: newEntries.map { entry($0, jackets: jackets) }
                ),
                BestTablePosterSection(
                    id: "B", title: "B\(bestCount)", average: bestAverage, accent: BestTablePosterLayout.bestAccent,
                    entries: bestEntries.map { entry($0, jackets: jackets) }
                ),
            ].filter { !$0.entries.isEmpty },
            versionText: version,
            // Resolve the mark ahead of rendering: asset-catalog lookups inside ImageRenderer can draw blank.
            appIcon: UIImage(named: "OnboardingIcon").map { Image(uiImage: $0) },
            colorScheme: colorScheme
        )
        // Same raster cap as the Android exporter: 2x of the logical canvas.
        let renderer = ImageRenderer(content: poster)
        renderer.proposedSize = ProposedViewSize(width: BestTablePosterView.canvasWidth, height: nil)
        renderer.scale = BestTablePosterLayout.outputScale
        guard let image = renderer.uiImage else { throw CocoaError(.fileWriteUnknown) }
        return try ExportFile.write(image)
    }

    private static func jacket(_ imageName: String, catalog: CatalogStore) async -> Image? {
        guard let fileURL = catalog.jacketURL(for: imageName),
              let data = try? await ExportFile.data(from: fileURL),
              let image = UIImage(data: data) else { return nil }
        return Image(uiImage: image.preparingThumbnail(of: CGSize(width: 186, height: 186)) ?? image)
    }

    private static func entry(_ entry: BestTableEntry, jackets: [String: Image]) -> BestTablePosterEntry {
        BestTablePosterEntry(
            id: entry.chartId,
            title: entry.title,
            difficulty: entry.difficulty,
            score: Int(entry.score),
            rank: entry.rank,
            rating: entry.rating,
            level: String(format: "%.1f", entry.constant),
            badges: badges(entry),
            jacket: jackets[entry.imageName]
        )
    }

    /// Same badge order and labels as the Android exporter: CLEAR, FULL COMBO, FULL CHAIN.
    private static func badges(_ entry: BestTableEntry) -> [BestTablePosterBadge] {
        var badges: [BestTablePosterBadge] = []
        if let name = ClearType.companion.displayName(value: entry.clear) {
            badges.append(BestTablePosterBadge(id: "clear", text: name, tint: BestTablePosterColors.clear(entry.clear)))
        }
        if let name = FullComboType.companion.displayName(value: entry.fullCombo), !name.isEmpty {
            badges.append(BestTablePosterBadge(id: "combo", text: name, tint: BestTablePosterColors.combo(entry.fullCombo)))
        }
        if let name = FullChainType.companion.displayName(value: entry.fullChain), !name.isEmpty {
            badges.append(BestTablePosterBadge(id: "chain", text: tr(name), tint: BestTablePosterColors.chain(entry.fullChain)))
        }
        return badges
    }
}
