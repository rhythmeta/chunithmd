import Foundation

struct ScannerEntrySelection: Identifiable {
    let id = UUID()
    let song: CatalogSongViewData
    let sheet: CatalogSongViewData.Sheet
    let result: ScannerResult
    let profileID: String
    let region: String
}
