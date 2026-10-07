import SwiftUI
import Observation

@MainActor @Observable
final class SongNavigation {
    var song: CatalogSongViewData?
    private(set) var preferredSheet: String?
    private(set) var sourceID = UUID()

    func open(_ song: CatalogSongViewData, sheetID: String? = nil, sourceID: UUID) {
        guard self.song == nil else { return }
        preferredSheet = sheetID
        self.sourceID = sourceID
        self.song = song
    }
}
