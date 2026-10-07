import Foundation
import CoreGraphics

@main
struct ScannerOrientationChecks {
    static func main() {
        typealias Orientation = ScannerPhysicalOrientation
        // The interface can stay portrait (including rotation lock) for either hold.
        let left = Orientation.portrait.updated(gravityX: -0.98, gravityY: 0.03)
        precondition(left == .landscapeLeft && left.rawValue == 1)
        let right = left.updated(gravityX: 0.98, gravityY: -0.03)
        precondition(right == .landscapeRight && right.rawValue == -1)
        precondition(right.updated(gravityX: 0.1, gravityY: -0.95) == .portrait)
        precondition(left.updated(gravityX: -0.1, gravityY: 0.95) == .portrait)
        // Tilting toward the result screen still counts as landscape.
        precondition(Orientation.portrait.updated(gravityX: -0.65, gravityY: 0.15) == .landscapeLeft)
        // Flat, diagonal and invalid samples cannot invent a rotation or flap the prompt.
        for previous in [Orientation.portrait, .landscapeLeft, .landscapeRight] {
            precondition(previous.updated(gravityX: 0.03, gravityY: -0.04) == previous)
            precondition(previous.updated(gravityX: 0.70, gravityY: -0.69) == previous)
            precondition(previous.updated(gravityX: .nan, gravityY: -1) == previous)
            precondition(previous.updated(gravityX: 1, gravityY: .infinity) == previous)
        }
        let box = CGRect(x: 0.1, y: 0.2, width: 0.3, height: 0.4)
        let source = CGSize(width: 1920, height: 1080), viewport = CGSize(width: 400, height: 800)
        // The camera preview crops 25 points from either side of a 450 × 800 image.
        check(ScannerOverlayGeometry.rect(box, imageSize: source, viewport: viewport,
            orientation: .landscapeLeft, aspectFill: true), CGRect(x: 155, y: 80, width: 180, height: 240))
        check(ScannerOverlayGeometry.rect(box, imageSize: source, viewport: viewport,
            orientation: .landscapeRight, aspectFill: true), CGRect(x: 65, y: 480, width: 180, height: 240))
        // Imported images keep their upright coordinates and centered letterboxing.
        check(ScannerOverlayGeometry.rect(box, imageSize: source, viewport: viewport,
            orientation: .portrait, aspectFill: false), CGRect(x: 40, y: 332.5, width: 120, height: 90))
        check(ScannerOverlayGeometry.rect(CGRect(x: 0, y: 0, width: 1, height: 1), imageSize: source,
            viewport: viewport, orientation: .portrait, aspectFill: false), CGRect(x: 0, y: 287.5, width: 400, height: 225))
        check(ScannerOverlayGeometry.rect(box, imageSize: .zero, viewport: viewport,
            orientation: .portrait, aspectFill: false), .zero)
        print("Scanner checks passed: physical orientation, both camera rotations, preview cropping and photo letterboxing.")
    }

    static func check(_ actual: CGRect, _ expected: CGRect) {
        precondition(abs(actual.minX - expected.minX) < 0.0001 && abs(actual.minY - expected.minY) < 0.0001
            && abs(actual.width - expected.width) < 0.0001 && abs(actual.height - expected.height) < 0.0001,
            "Unexpected overlay rectangle: \(actual), expected \(expected)")
    }
}
