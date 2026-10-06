import SwiftUI

struct SongRow: View {
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @Environment(\.colorScheme) private var scheme
    let song: CatalogSongViewData
    var subtitle: String? = nil
    var preferredSheet: String? = nil
    var card = false
    var showsProgress = false
    var scrollsText = false
    @State private var identity = UUID()

    var body: some View {
        let sheet = song.sheets.first { $0.id == preferredSheet } ?? song.sheets.last
        let accent = difficultyStyle(sheet?.difficulty ?? "master", type: sheet?.type, vertical: true)
        Button { navigation.open(song, sheetID: preferredSheet, sourceID: identity) } label: {
            HStack(spacing: 0) {
                RoundedRectangle(cornerRadius: 2).fill(accent).frame(width: 4).padding(.vertical, 8)
                HStack(spacing: 14) {
                    JacketImage(url: catalog.jacketURL(for: song.imageName))
                        .frame(width: card ? 52 : 56, height: card ? 52 : 56)
                        .clipShape(.rect(cornerRadius: card ? 12 : 10))
                    VStack(alignment: .leading, spacing: 3) {
                        if scrollsText {
                            SongDetailText(text: song.title, font: .system(size: 15, weight: .semibold), lineHeight: 20, alignment: .leading)
                            SongDetailText(text: subtitle ?? song.artist, font: .system(size: 12), color: .secondary, lineHeight: 16, alignment: .leading)
                        } else {
                            Text(song.title).font(.system(size: 15, weight: .semibold)).lineLimit(1)
                            Text(subtitle ?? song.artist).font(.system(size: 12)).foregroundStyle(.secondary).lineLimit(subtitle == nil ? 1 : 2)
                        }
                    }
                    Spacer(minLength: 0)
                    VStack(alignment: .trailing, spacing: 6) {
                        if let version = song.version { VersionBadge(version: version, dark: scheme == .dark) }
                        if showsProgress {
                            SongProgressDots(song: song)
                        } else {
                            HStack(spacing: 3) {
                                ForEach(preferredSheet == nil ? Array(song.sheets.reversed()) : song.sheets.filter { $0.id == preferredSheet }) { item in
                                    let style = item.type.lowercased() == "we" ? AnyShapeStyle(WorldsEndStyle.ring) : difficultyStyle(item.difficulty)
                                    Circle().stroke(style.opacity(0.65), lineWidth: 1.2).frame(width: 8, height: 8)
                                }
                            }
                        }
                    }
                }.padding(.leading, 10).padding(.trailing, card ? 14 : 0)
            }
            .padding(.vertical, card ? 12 : 2)
            .background { if card { RoundedRectangle(cornerRadius: 14).fill(.ultraThinMaterial) } }
            .overlay { if card { RoundedRectangle(cornerRadius: 14).stroke(accent.opacity(0.12)) } }
            .contentShape(.rect)
        }.buttonStyle(.plain).accessibilityIdentifier("song-row-" + song.id)
            .modifier(SongTransitionSource(id: identity))
    }
}
