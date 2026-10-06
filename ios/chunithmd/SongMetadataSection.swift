import Shared
import SwiftUI

struct SongMetadataSection: View {
    let song: CatalogSongViewData
    let onCopy: (String) -> Void

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 8) { SongMetadataItems(song: song, grid: false, onCopy: onCopy) }.fixedSize(horizontal: true, vertical: false)
            LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                SongMetadataItems(song: song, grid: true, onCopy: onCopy)
            }
        }
    }
}
