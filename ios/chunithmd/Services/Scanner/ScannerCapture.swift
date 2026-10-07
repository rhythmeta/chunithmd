import Foundation
import CoreGraphics

nonisolated struct ScannerCapture: Sendable {
    let observationsJSON: String
    let previewData: Data
    let boxes: [ScannerDetectedBox]
    let imageSize: CGSize
}
