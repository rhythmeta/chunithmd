import SwiftUI
import Shared

struct SongLinksSection: View {
    let song: CatalogSongViewData
    @Environment(CatalogStore.self) private var catalog
    var body: some View {
        let source = catalog.bundle?.catalog.songs.first { $0.songId == song.id }
        let regions = source.map { NativeCatalogQuery.shared.availableRegions(song: $0) } ?? []
        VStack(spacing: 20) {
            HStack(spacing: 12) {
                ForEach(["jp", "intl", "cn"], id: \.self) { region in
                    VStack(spacing: 3) {
                        Text(region == "jp" ? "🇯🇵" : region == "cn" ? "🇨🇳" : "🌏").font(.system(size: 22))
                            .opacity(regions.contains(region) ? 1 : 0.25).saturation(regions.contains(region) ? 1 : 0)
                        Text(region == "jp" ? tr("日本") : region == "cn" ? tr("中国") : tr("国际")).font(.system(size: 9, weight: .medium))
                            .foregroundStyle(regions.contains(region) ? AnyShapeStyle(.primary) : AnyShapeStyle(.secondary.opacity(0.4)))
                    }
                    .accessibilityLabel("\(AppTheme.serverName(region))，\(regions.contains(region) ? tr("可游玩") : tr("未收录"))")
                }
                Spacer()
            }.padding(.horizontal, 16).padding(.vertical, 14)
                .background(.ultraThinMaterial, in: .rect(cornerRadius: 14))
                .overlay { RoundedRectangle(cornerRadius: 14).strokeBorder(.primary.opacity(0.06)) }
            HStack(spacing: 12) {
                Image(systemName: "magnifyingglass").foregroundStyle(.secondary).font(.system(size: 12))
                if let youtube = searchURL(base: "https://www.youtube.com/results", parameter: "search_query") {
                    Link(destination: youtube) { Label("YouTube", systemImage: "play.rectangle.fill").foregroundStyle(.red).padding(.horizontal, 10).padding(.vertical, 5).background(.red.opacity(0.08), in: .capsule) }
                }
                if let bilibili = searchURL(base: "https://search.bilibili.com/all", parameter: "keyword") {
                    Link(destination: bilibili) { Label("Bilibili", systemImage: "video.fill").foregroundStyle(.cyan).padding(.horizontal, 10).padding(.vertical, 5).background(.cyan.opacity(0.08), in: .capsule) }
                }
                Spacer(minLength: 0)
            }.font(.system(size: 11, weight: .semibold)).padding(.horizontal, 16).padding(.vertical, 12)
                .background(.ultraThinMaterial, in: .rect(cornerRadius: 16))
                .overlay { RoundedRectangle(cornerRadius: 16).strokeBorder(.primary.opacity(0.06)) }
        }
    }
    private func searchURL(base: String, parameter: String) -> URL? {
        var url = URLComponents(string: base)
        url?.queryItems = [URLQueryItem(name: parameter, value: "CHUNITHM " + song.title)]
        return url?.url
    }
}
