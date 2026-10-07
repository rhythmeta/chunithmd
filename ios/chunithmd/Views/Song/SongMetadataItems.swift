import Shared
import SwiftUI

struct SongMetadataItems: View {
    let song: CatalogSongViewData
    let grid: Bool
    let onCopy: (String) -> Void

    var body: some View {
        if let bpm = song.bpm {
            let value = bpm.formatted(.number.precision(.fractionLength(0...1)))
            SongMetadataPill(icon: "metronome", value: value, label: "BPM", fillsWidth: grid)
                .modifier(SongDetailCopyAction(text: value, onCopy: onCopy))
                .accessibilityIdentifier("song-copy-bpm")
        }
        SongMetadataPill(icon: "square.grid.2x2", value: song.category, fillsWidth: grid)
            .modifier(SongDetailCopyAction(text: song.category, onCopy: onCopy))
            .accessibilityIdentifier("song-copy-category")
        if let version = song.version {
            SongMetadataPill(icon: "clock", value: CatalogVersionFormatter.shared.badge(version: version), fillsWidth: grid)
                .modifier(SongDetailCopyAction(text: version, onCopy: onCopy))
                .accessibilityIdentifier("song-copy-version")
        }
        if let date = song.releaseDate {
            let parts = date.split(separator: "-")
            let compact = parts.count == 3 ? "\(parts[0].suffix(2))/\(parts[1])/\(parts[2])" : date
            SongMetadataPill(icon: "calendar", value: grid ? date : compact, fillsWidth: grid)
                .modifier(SongDetailCopyAction(text: date, onCopy: onCopy))
                .accessibilityIdentifier("song-copy-date")
        }
    }
}
