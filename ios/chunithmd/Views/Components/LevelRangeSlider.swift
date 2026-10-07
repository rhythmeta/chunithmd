import SwiftUI

struct LevelRangeSlider: View {
    @Binding var lower: Double
    @Binding var upper: Double
    let active: Bool
    @State private var lowerHandle: Bool?
    var body: some View {
        GeometryReader { geometry in
            let width = max(1, geometry.size.width - 24)
            let low = (lower - 1) / 15 * width
            let high = (upper - 1) / 15 * width
            ZStack(alignment: .leading) {
                Capsule().fill(.primary.opacity(0.1)).frame(height: 4)
                Capsule().fill(active ? .blue : .gray.opacity(0.5)).frame(width: max(0, high - low), height: 4).offset(x: low)
                Circle().fill(.white).frame(width: 24, height: 24).shadow(radius: 2, y: 1).offset(x: low - 12)
                Circle().fill(.white).frame(width: 24, height: 24).shadow(radius: 2, y: 1).offset(x: high - 12)
            }.padding(.horizontal, 12).frame(height: 44).contentShape(.rect)
                .gesture(DragGesture(minimumDistance: 0).onChanged { value in
                    let proposed = min(16, max(1, ((1 + (value.location.x - 12) / width * 15) * 10).rounded() / 10))
                    if lowerHandle == nil {
                        lowerHandle = abs(proposed - lower) == abs(proposed - upper) ? value.translation.width < 0 : abs(proposed - lower) < abs(proposed - upper)
                    }
                    if lowerHandle == true { lower = min(proposed, upper) } else { upper = max(proposed, lower) }
                }.onEnded { _ in lowerHandle = nil })
        }.frame(height: 44)
            .accessibilityRepresentation {
                VStack {
                    Slider(value: $lower, in: 1...upper, step: 0.1) { Text(tr("最低定数")) }
                    Slider(value: $upper, in: lower...16, step: 0.1) { Text(tr("最高定数")) }
                }
            }
    }
}
