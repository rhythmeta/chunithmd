import SwiftUI

struct SongDetailText: View {
    let text: String
    let font: Font
    var color: Color = .primary
    var lineHeight = 32.0
    var alignment: Alignment = .center
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var textWidth = 0.0
    @State private var start = Date()

    var body: some View {
        Group {
            if reduceMotion || dynamicTypeSize.isAccessibilitySize {
                Text(text).font(font).foregroundStyle(color).multilineTextAlignment(alignment == .leading ? .leading : .center)
            } else {
                GeometryReader { proxy in
                    let width = proxy.size.width.isFinite ? max(0, proxy.size.width) : 0
                    let scrolling = textWidth > width
                    let distance = textWidth + 60
                    TimelineView(.animation(paused: !scrolling)) { context in
                        let phase = max(0, context.date.timeIntervalSince(start)).truncatingRemainder(dividingBy: distance / 30 + 2)
                        HStack(spacing: 60) {
                            Text(text).fixedSize().onGeometryChange(for: Double.self) { $0.size.width } action: { textWidth = $0 }
                            if scrolling { Text(text).fixedSize().accessibilityHidden(true) }
                        }
                        .font(font).foregroundStyle(color)
                        .offset(x: scrolling ? -max(0, phase - 2) * 30 : 0)
                        .frame(width: width, alignment: scrolling ? .leading : alignment)
                    }
                    .onChange(of: proxy.size.width) { start = .now }
                }.frame(height: lineHeight).clipped()
            }
        }
        .onChange(of: text) { start = .now }
        .accessibilityElement(children: .ignore).accessibilityLabel(text)
    }
}
