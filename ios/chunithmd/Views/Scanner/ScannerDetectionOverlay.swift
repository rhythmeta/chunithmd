import SwiftUI

struct ScannerDetectionOverlay: View {
    let boxes: [ScannerDetectedBox]
    let imageSize: CGSize
    let orientation: ScannerPhysicalOrientation
    let aspectFill: Bool

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .topLeading) {
                ForEach(boxes) { box in
                    let rect = ScannerOverlayGeometry.rect(box.rect, imageSize: imageSize, viewport: geometry.size,
                        orientation: orientation, aspectFill: aspectFill)
                    Path { $0.addRect(rect) }.stroke(.green, lineWidth: 2)
                    Text(label(for: box.field)).font(.caption2.bold()).foregroundStyle(.black)
                        .padding(.horizontal, 4).padding(.vertical, 2).background(.green, in: .rect(cornerRadius: 3))
                        .position(x: min(max(40, rect.midX), max(40, geometry.size.width - 40)),
                            y: min(max(12, rect.minY - 12), max(12, geometry.size.height - 12)))
                }
            }.clipped()
        }
        .allowsHitTesting(false).accessibilityHidden(true)
    }

    private func label(for field: String) -> String {
        switch field {
        case "title": tr("曲名")
        case "difficulty": tr("难度")
        case "level": tr("等级")
        case "score": tr("分数")
        case "clear": tr("CLEAR 状态")
        case "combo": tr("COMBO 状态")
        default: field
        }
    }
}
