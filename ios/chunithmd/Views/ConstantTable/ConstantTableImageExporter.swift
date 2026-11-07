import Shared
import SwiftUI

/// Renders the whole constant table into one PNG and hands it to the system share sheet.
///
/// Nothing is paged and the height is unlimited: a long table becomes a single tall image
/// instead of several share targets. Width matches `ConstantTablePosterView` (1440pt at 1x).
enum ConstantTableImageExporter {
    static let canvasWidth: CGFloat = 1440

    @MainActor
    static func render(
        catalog: CatalogStore,
        title: String,
        subtitle: String,
        sections: [ConstantTableSection],
        includesScores: Bool,
        profileName: String?,
        colorScheme: ColorScheme
    ) async throws -> URL {
        var images: [String: UIImage] = [:]
        for section in sections {
            for entry in section.entries where images[entry.imageName] == nil {
                try Task.checkCancellation()
                if let image = await jacket(entry.imageName, catalog: catalog) { images[entry.imageName] = image }
            }
        }
        let poster = ConstantTablePosterView(
            title: title,
            subtitle: subtitle,
            sections: sections,
            images: images,
            includesScores: includesScores,
            profileName: profileName
        )
        .environment(\.colorScheme, colorScheme)
        let renderer = ImageRenderer(content: poster)
        renderer.proposedSize = ProposedViewSize(width: canvasWidth, height: nil)
        renderer.scale = 1
        guard let image = renderer.uiImage else { throw CocoaError(.fileWriteUnknown) }
        return try ExportFile.write(image)
    }

    private static func jacket(_ imageName: String, catalog: CatalogStore) async -> UIImage? {
        guard let url = catalog.jacketURL(for: imageName),
              let data = try? await ExportFile.data(from: url),
              let image = UIImage(data: data) else { return nil }
        // Tiles draw the jacket at 58pt; a 72pt thumbnail is plenty and keeps a full table cheap.
        return image.preparingThumbnail(of: CGSize(width: 72, height: 72)) ?? image
    }
}
