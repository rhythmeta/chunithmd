import SwiftUI

struct CommunitySongHeader: View {
    let song: CatalogSongViewData?
    let songID: String
    let count: Int
    let showsSongLinks: Bool
    @Environment(CatalogStore.self) private var catalog
    @Environment(SongNavigation.self) private var navigation
    @State private var identity = UUID()

    var body: some View {
        Button {
            if let song { navigation.open(song, sourceID: identity) }
        } label: {
            HStack(spacing: 12) {
                JacketImage(url: song.flatMap { catalog.jacketURL(for: $0.imageName) })
                    .frame(width: 46, height: 46).clipShape(.rect(cornerRadius: 9))
                VStack(alignment: .leading, spacing: 3) {
                    Text(song?.title ?? songID).font(.system(size: 14, weight: .semibold)).lineLimit(1)
                        .foregroundStyle(.primary)
                    Text(tr("{0} 个候选别名", count)).font(.system(size: 11, weight: .medium)).foregroundStyle(.secondary)
                }
                Spacer()
            }.padding(.vertical, 4).contentShape(.rect)
        }
        .buttonStyle(.plain).disabled(song == nil || !showsSongLinks)
        .textCase(nil)
        .modifier(SongTransitionSource(id: identity))
        .accessibilityIdentifier("community-song-" + songID)
    }
}
