import Foundation

struct ScannerPresentation: Decodable {
    let review: ScannerReviewData
    let match: ScannerReviewData.Candidate?
    let accepted: Bool
    let shouldClear: Bool
}
