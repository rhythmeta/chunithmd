import Foundation

struct ScannerSongPresentation: Decodable {
    struct Match: Decodable {
        let songId: String
        let title: String
        let similarity: Double
    }
    let match: Match?
    let accepted: Bool
    let shouldClear: Bool
}
