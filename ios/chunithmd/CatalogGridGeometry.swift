import CoreGraphics
import Foundation

/// The Android photo lattice: fixed gaps, centered columns, continuous square size.
struct CatalogGridGeometry {
    let width: Double
    let count: Int
    let zoom: Double
    let gap = 2.0
    var columns: Int { zoom < 1.5 ? 5 : 3 }
    var size: Double { sizeFor(5) + (sizeFor(3) - sizeFor(5)) * (zoom - 1) }
    var step: Double { size + gap }
    var height: Double { max(0, Double((count + columns - 1) / columns) * step - gap) }
    func sizeFor(_ columns: Int) -> Double { max(1, (width - gap * Double(columns - 1)) / Double(columns)) }
    func frame(_ index: Int) -> CGRect {
        let origin = (width - size) / 2 - Double(columns / 2) * step
        return CGRect(x: origin + Double(index % columns) * step, y: Double(index / columns) * step, width: size, height: size)
    }
    func visible(top: Double, height: Double) -> Range<Int> {
        let first = max(0, Int(floor(top / step)) - 2) * columns
        let last = min(count, (max(0, Int(ceil((top + height) / step))) + 2) * columns)
        return min(first, last)..<last
    }
}
