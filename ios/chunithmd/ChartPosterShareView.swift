import SwiftUI

struct ChartPosterShareView: View {
    let title: String
    let subtitle: String
    let entries: [PosterEntry]
    @Environment(\.dismiss) private var dismiss
    @Environment(CatalogStore.self) private var catalog
    @State private var previews: [UIImage] = []
    @State private var urls: [URL] = []
    @State private var error: String?
    var body: some View {
        NavigationStack {
            Group {
                if !previews.isEmpty {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(previews.indices, id: \.self) { index in
                                Image(uiImage: previews[index]).resizable().scaledToFit()
                                    .accessibilityLabel(tr("{0}分享图片，第 {1} 页", title, index + 1))
                                    .accessibilityIdentifier("chart-poster-preview")
                            }
                        }.padding()
                    }
                } else if let error { ContentUnavailableView(tr("无法生成图片"), systemImage: "exclamationmark.triangle", description: Text(error)) }
                else { ProgressView(tr("正在生成分享图片…")) }
            }.navigationTitle(tr("分享图片")).navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button(tr("完成")) { dismiss() } }
                    if !urls.isEmpty {
                        ToolbarItem(placement: .confirmationAction) {
                            ShareLink(items: urls).labelStyle(.iconOnly).accessibilityIdentifier("poster-share")
                        }
                    }
                }
        }.task { await render() }
    }
    private func render() async {
        var files: [URL] = []
        do {
            // Share extensions may read the files after this sheet closes; expire old exports later.
            let previous = try FileManager.default.contentsOfDirectory(at: .temporaryDirectory, includingPropertiesForKeys: [.contentModificationDateKey])
            for file in previous where file.lastPathComponent.hasPrefix("chunithmd-") && file.pathExtension == "png" {
                if let modified = try? file.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate,
                   modified < Date.now.addingTimeInterval(-86_400) {
                    try? FileManager.default.removeItem(at: file)
                }
            }
            var images: [String: UIImage] = [:]
            for entry in entries where images[entry.imageName] == nil {
                try Task.checkCancellation()
                if let url = catalog.jacketURL(for: entry.imageName), let data = try? await Self.load(url), let image = UIImage(data: data) {
                    images[entry.imageName] = image.preparingThumbnail(of: CGSize(width: 176, height: 176)) ?? image
                }
            }
            // Bound image height and memory for large constant tables. Best 50 fits on one page.
            let pages = stride(from: 0, to: max(1, entries.count), by: 60).map {
                Array(entries[$0..<min($0 + 60, entries.count)])
            }
            var rendered: [UIImage] = []
            for (index, page) in pages.enumerated() {
                try Task.checkCancellation()
                let pageSubtitle = pages.count > 1 ? "\(subtitle) · \(index + 1)/\(pages.count)" : subtitle
                let renderer = ImageRenderer(content: ChartPosterView(title: title, subtitle: pageSubtitle, entries: page, images: images))
                renderer.proposedSize = ProposedViewSize(width: 1000, height: nil)
                renderer.scale = 1
                guard let image = renderer.uiImage, let bytes = image.pngData() else { throw CocoaError(.fileWriteUnknown) }
                let file = URL.temporaryDirectory.appending(path: "chunithmd-\(UUID().uuidString).png")
                try bytes.write(to: file, options: .atomic)
                rendered.append(image.preparingThumbnail(of: CGSize(width: 500, height: 2000)) ?? image)
                files.append(file)
                await Task.yield()
            }
            previews = rendered
            urls = files
        } catch {
            for file in files { try? FileManager.default.removeItem(at: file) }
            if !Task.isCancelled { self.error = error.localizedDescription }
        }
    }
    @concurrent private static func load(_ url: URL) async throws -> Data {
        if url.isFileURL { return try Data(contentsOf: url) }
        return try await URLSession.shared.data(from: url).0
    }
}
