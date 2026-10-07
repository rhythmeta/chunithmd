import Foundation

struct ScannerResult: Identifiable {
    let id = UUID()
    let match: ScannerReviewData.Candidate
    let score: Int
    let clear: String
    let photoData: Data
}
