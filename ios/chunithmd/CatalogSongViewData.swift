import Foundation

struct CatalogSongViewData: Decodable, Identifiable, Hashable {
    struct Sheet: Decodable, Hashable, Identifiable {
        let type: String
        let difficulty: String
        let level: String
        let levelValue: Double?
        let regions: [String: Bool]?
        let noteDesigner: String?
        let internalLevelValue: Double?
        let noteCounts: Notes?
        var id: String { type + ":" + difficulty }
        struct Notes: Decodable, Hashable {
            let tap: Int?; let hold: Int?; let slide: Int?; let air: Int?; let flick: Int?; let total: Int?
        }
    }

    let songId: String
    let title: String
    let artist: String
    let category: String
    let imageName: String
    let bpm: Double?
    let releaseDate: String?
    let version: String?
    let sheets: [Sheet]

    var id: String { songId }
}

