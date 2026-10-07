import Foundation
import CoreGraphics

/// Model boxes use normalized coordinates in the upright image, with a top-left origin.
nonisolated struct ScannerDetectedBox: Identifiable, Sendable {
    let field: String
    let rect: CGRect
    var id: String { field }
}
