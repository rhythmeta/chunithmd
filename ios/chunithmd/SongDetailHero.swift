import SwiftUI
import Shared

struct SongDetailHero: View {
    let song: CatalogSongViewData
    let onCopy: (String) -> Void
    @Environment(CatalogStore.self) private var catalog
    @Environment(RhythmetaAccountStore.self) private var account

    var body: some View {
        let community = account.community.approvedAliases[song.id] ?? []
        let aliases = Array(Set((catalog.bundle?.aliases[song.id] ?? []) + community)).sorted()
        VStack(spacing: 16) {
            JacketImage(url: catalog.jacketURL(for: song.imageName), allowsSharing: true)
                .frame(width: 220, height: 220).clipShape(.rect(cornerRadius: 28))
                .shadow(color: .black.opacity(0.3), radius: 24, y: 12)
            VStack(spacing: 6) {
                SongDetailText(text: song.title, font: .title2.bold())
                    .modifier(SongDetailCopyAction(text: song.title, onCopy: onCopy))
                    .accessibilityIdentifier("song-copy-title").padding(.horizontal, 32)
                SongDetailText(text: song.artist, font: .subheadline, color: .secondary, lineHeight: 20)
                    .modifier(SongDetailCopyAction(text: song.artist, onCopy: onCopy))
                    .accessibilityIdentifier("song-copy-artist").padding(.horizontal, 32)
                if !aliases.isEmpty {
                    ScrollView(.horizontal) {
                        HStack(spacing: 6) {
                            ForEach(aliases, id: \.self) { alias in
                                let isCommunity = community.contains(alias)
                                Text(alias).font(.system(size: 11, weight: .medium))
                                    .foregroundStyle(isCommunity ? .primary : .secondary)
                                    .padding(.horizontal, 8).padding(.vertical, 4)
                                    .background(.secondary.opacity(isCommunity ? 0.06 : 0.1), in: .capsule)
                                    .overlay {
                                        if isCommunity { Capsule().strokeBorder(.blue.opacity(0.55), style: StrokeStyle(lineWidth: 1, dash: [4, 3])) }
                                    }
                                    .modifier(SongDetailCopyAction(text: alias, onCopy: onCopy))
                            }
                        }.padding(.horizontal, 32)
                    }.scrollIndicators(.hidden).padding(.top, 4)
                }
            }
        }.padding(.top, 8)
    }
}
