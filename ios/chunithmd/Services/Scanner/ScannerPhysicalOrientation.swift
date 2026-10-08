import Foundation
import ImageIO

/// Device-space gravity, independent of the interface orientation and rotation lock.
/// Raw values also select the camera buffer's existing upright / upside-down transform.
nonisolated enum ScannerPhysicalOrientation: Int {
    case portrait = 0
    case landscapeLeft = 1
    case landscapeRight = -1

    var imageOrientation: CGImagePropertyOrientation {
        switch self {
        case .portrait: .right
        case .landscapeLeft: .up
        case .landscapeRight: .down
        }
    }

    func updated(gravityX x: Double, gravityY y: Double) -> Self {
        guard x.isFinite, y.isFinite else { return self }
        // Ignore flat poses and keep a dead band around diagonal holds to avoid flicker.
        if abs(x) >= 0.55 && abs(x) > abs(y) + 0.12 {
            return x < 0 ? .landscapeLeft : .landscapeRight
        }
        if abs(y) >= 0.55 && abs(y) > abs(x) + 0.12 { return .portrait }
        return self
    }
}
