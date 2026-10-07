import SwiftUI

struct CollectionPreviewRow: View {
    let items: [PersonalSnapshot.Item]
    @Environment(CatalogStore.self) private var catalog

    var body: some View {
        let songs = Dictionary(catalog.allSongs.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        HStack(spacing: 8) {
            ForEach(Array(items.filter { songs[$0.songId] != nil }.prefix(4))) { item in
                if let song = songs[item.songId] {
                    JacketImage(url: catalog.jacketURL(for: song.imageName))
                        .frame(width: 56, height: 56).clipShape(.rect(cornerRadius: 10))
                        .overlay { RoundedRectangle(cornerRadius: 10).strokeBorder(difficultyStyle(item.difficulty, type: item.chartType), lineWidth: 2) }
                }
            }
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
}
