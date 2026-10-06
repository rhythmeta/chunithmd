import SwiftUI

struct SongTile: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    let song: CatalogSongViewData
    var radius = 0.0
    var caption: String? = nil
    var preferredSheet: String? = nil
    var showsDifficultyBorder = false
    @State private var identity = UUID()

    var body: some View {
        Button { navigation.open(song, sheetID: preferredSheet, sourceID: identity) } label: {
            JacketImage(url: catalog.jacketURL(for: song.imageName))
                .aspectRatio(1, contentMode: .fit)
                .clipShape(.rect(cornerRadius: radius))
                .overlay {
                    if showsDifficultyBorder, let sheet = song.sheets.first(where: { $0.id == preferredSheet }) {
                        RoundedRectangle(cornerRadius: radius).strokeBorder(difficultyStyle(sheet.difficulty, type: sheet.type), lineWidth: 2)
                    }
                }
                .overlay(alignment: .bottomTrailing) {
                    if showsDifficultyBorder {
                        SongProgressDots(song: song, preferredSheet: preferredSheet)
                            .padding(.horizontal, radius > 6 ? 6 : 4).padding(.vertical, radius > 6 ? 3 : 2)
                            .background(.ultraThickMaterial, in: .capsule).environment(\.colorScheme, .light)
                            .padding(radius > 6 ? 6 : 4)
                    }
                    if let caption {
                        Text(caption).font(.caption.bold()).padding(4)
                            .foregroundStyle(.white).background(.black.opacity(0.7), in: .rect(cornerRadius: 4))
                    }
                }
        }
        .buttonStyle(.plain)
        .modifier(SongTransitionSource(id: identity))
        .accessibilityIdentifier("song-tile-" + song.id)
        .accessibilityLabel(song.title + (caption.map { ", " + $0 } ?? ""))
    }
}
