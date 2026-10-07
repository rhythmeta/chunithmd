import Foundation

nonisolated struct ScannerModelFiles: Decodable, Sendable {
    let revision: String
    let directory: String
}
