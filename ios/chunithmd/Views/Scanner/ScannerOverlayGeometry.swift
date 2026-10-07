import CoreGraphics

/// Maps upright detector coordinates to the portrait camera's aspect-fill preview,
/// or to an imported image's centered aspect-fit preview.
nonisolated enum ScannerOverlayGeometry {
    static func rect(_ box: CGRect, imageSize: CGSize, viewport: CGSize,
                     orientation: ScannerPhysicalOrientation, aspectFill: Bool) -> CGRect {
        guard imageSize.width > 0, imageSize.height > 0, viewport.width > 0, viewport.height > 0 else { return .zero }
        let normalized: CGRect
        let source: CGSize
        switch orientation {
        case .portrait:
            normalized = box
            source = imageSize
        case .landscapeLeft:
            normalized = CGRect(x: 1 - box.maxY, y: box.minX, width: box.height, height: box.width)
            source = CGSize(width: imageSize.height, height: imageSize.width)
        case .landscapeRight:
            normalized = CGRect(x: box.minY, y: 1 - box.maxX, width: box.height, height: box.width)
            source = CGSize(width: imageSize.height, height: imageSize.width)
        }
        let scaleX = viewport.width / source.width, scaleY = viewport.height / source.height
        let scale = aspectFill ? max(scaleX, scaleY) : min(scaleX, scaleY)
        let width = source.width * scale, height = source.height * scale
        return CGRect(x: (viewport.width - width) / 2 + normalized.minX * width,
            y: (viewport.height - height) / 2 + normalized.minY * height,
            width: normalized.width * width, height: normalized.height * height)
    }
}
