import Foundation

// Compiled together with CatalogGridGeometry.swift by scripts/check-ios-grid.sh.
@main struct GridGeometryChecks {
    static func main() {
        for width in [320.0, 402.0, 768.0, 1024.0] {
            for zoom in stride(from: 1.0, through: 2.0, by: 0.05) {
                let grid = CatalogGridGeometry(width: width, count: 1995, zoom: zoom)
                assert(grid.size > 0)
                let center = grid.frame(grid.columns / 2)
                assert(abs(center.midX - width / 2) < 0.0001)
                assert(grid.visible(top: 0, height: 874).count < 150)
                assert(grid.visible(top: grid.height + 1000, height: 874).isEmpty)
                assert(grid.frame(0).width == grid.frame(0).height)
            }
            for zoom in [1.0, 2.0] {
                let grid = CatalogGridGeometry(width: width, count: 1995, zoom: zoom)
                assert(abs(grid.frame(0).minX) < 0.0001)
                assert(abs(grid.frame(grid.columns - 1).maxX - width) < 0.0001)
            }
        }
        assert(CatalogGridGeometry(width: 402, count: 0, zoom: 1).visible(top: 0, height: 874).isEmpty)
        print("Grid geometry: centered, square, bounded and virtualized at all sizes.")
    }
}
