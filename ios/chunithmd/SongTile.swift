import SwiftUI

struct SongTile: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    let song: CatalogSongViewData
    var radius = 0.0
    var caption: String? = nil
    var preferredSheet: String? = nil
    @State private var identity = UUID()
    @State private var frame = CGRect.zero

    var body: some View {
        Button { navigation.open(song, from: frame, radius: radius, sheetID: preferredSheet, sourceID: identity) } label: {
            JacketImage(url: catalog.jacketURL(for: song.imageName))
                .aspectRatio(1, contentMode: .fit)
                .clipShape(.rect(cornerRadius: radius))
                .overlay(alignment: .bottomTrailing) {
                    if let caption {
                        Text(caption).font(.caption.bold()).padding(4)
                            .foregroundStyle(.white).background(.black.opacity(0.7), in: .rect(cornerRadius: 4))
                    }
                }
                .opacity(navigation.sourceID == identity && navigation.song != nil ? 0 : 1)
                .onGeometryChange(for: CGRect.self) { $0.frame(in: .global) } action: { frame = $0; if navigation.sourceID == identity { navigation.source = $0 } }
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("song-tile-" + song.id)
        .accessibilityLabel(song.title + (caption.map { ", " + $0 } ?? ""))
    }
}
