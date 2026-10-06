import Foundation
import Observation
import Shared

@MainActor @Observable
final class PersonalStore {
    let bridge = PersonalDataBridge(files: RhythmetaSnapshotFiles())
    private(set) var snapshot = PersonalSnapshot()
    private(set) var playRecords: [ScoreRecord] = []
    private(set) var best: [BestTableEntry] = []
    private(set) var rating = 0.0
    private(set) var revision = 0
    var error: String?

    func reload(catalog: CatalogStore) {
        do {
            snapshot = try JSONDecoder().decode(PersonalSnapshot.self, from: Data(try bridge.snapshotJson().utf8))
            playRecords = try bridge.playHistory()
            if let bundle = catalog.bundle {
                best = try bridge.bestEntries(bundle: bundle)
                rating = try bridge.rating(bundle: bundle).rating
            }
            catalog.server = snapshot.activeProfile?.server ?? "jp"
            catalog.favorites = snapshot.favoriteSongIds
            revision += 1
        } catch { self.error = error.localizedDescription }
    }

    func perform(catalog: CatalogStore, _ action: () throws -> Void) {
        do { try action(); reload(catalog: catalog) }
        catch { self.error = error.localizedDescription }
    }

    func history(songID: String, sheetID: String, sort: ScoreHistorySort = .time) -> [ScoreRecord] {
        bridge.chartHistory(records: playRecords, songId: songID, sheetId: sheetID, sort: sort)
    }
}
