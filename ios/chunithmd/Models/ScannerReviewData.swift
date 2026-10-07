import Foundation

struct ScannerReviewData: Decodable {
    struct Fields: Decodable {
        let title: String; let difficulty: String; let level: String
        let score: String; let clear: String; let combo: String
    }
    struct Candidate: Decodable, Identifiable {
        let songId: String; let title: String; let type: String
        let difficulty: String; let level: String; let similarity: Double
        var id: String { "\(songId):\(type):\(difficulty)" }
    }
    let fields: Fields
    let parsedScore: Int?
    let clear: String
    let combo: String
    let candidates: [Candidate]
    let recommendedKey: String?
}
