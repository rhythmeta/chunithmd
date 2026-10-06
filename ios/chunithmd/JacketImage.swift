import SwiftUI

struct JacketImage: View {
    let url: URL?
    var allowsSharing = false
    @State private var image: UIImage?
    @State private var failed = false
    private static let cache = NSCache<NSURL, UIImage>()

    var body: some View {
        Group {
            if let image { Image(uiImage: image).resizable().scaledToFill() }
            else {
                Rectangle().fill(.quaternary)
                    .overlay { Image(systemName: failed ? "photo" : "music.note").foregroundStyle(.secondary) }
            }
        }
        .clipped()
        .accessibilityHidden(!allowsSharing)
        .accessibilityLabel(tr("分享封面"))
        .contextMenu {
            if allowsSharing, let image {
                Button(tr("复制封面"), systemImage: "doc.on.doc") { UIPasteboard.general.image = image }
                ShareLink(item: Image(uiImage: image), preview: SharePreview(tr("分享封面"), image: Image(uiImage: image))) {
                    Label(tr("分享封面"), systemImage: "square.and.arrow.up")
                }
            }
        }
        .task(id: url) { await load() }
    }

    private func load() async {
        image = nil; failed = false
        guard let url else { return }
        if let cached = Self.cache.object(forKey: url as NSURL) { image = cached; return }
        do {
            let data = try await Self.data(for: url)
            try Task.checkCancellation()
            guard let decoded = UIImage(data: data) else { failed = true; return }
            Self.cache.countLimit = 300
            Self.cache.setObject(decoded, forKey: url as NSURL)
            image = decoded
        } catch { if !Task.isCancelled { failed = true } }
    }

    @concurrent private static func data(for url: URL) async throws -> Data {
        if url.isFileURL { return try Data(contentsOf: url) }
        return try await URLSession.shared.data(from: url).0
    }
}
