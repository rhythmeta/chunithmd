import SwiftUI

struct SongDetailText: View {
    let text: String
    let font: Font
    var color: Color = .primary
    var lineHeight = 32.0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var textWidth = 0.0
    @State private var start = Date()

    var body: some View {
        Group {
            if reduceMotion || dynamicTypeSize.isAccessibilitySize {
                Text(text).font(font).foregroundStyle(color).multilineTextAlignment(.center)
            } else {
                GeometryReader { proxy in
                    let scrolling = textWidth > proxy.size.width
                    let distance = textWidth + 60
                    TimelineView(.animation(paused: !scrolling)) { context in
                        let phase = max(0, context.date.timeIntervalSince(start)).truncatingRemainder(dividingBy: distance / 30 + 2)
                        HStack(spacing: 60) {
                            Text(text).fixedSize().onGeometryChange(for: Double.self) { $0.size.width } action: { textWidth = $0 }
                            if scrolling { Text(text).fixedSize().accessibilityHidden(true) }
                        }
                        .font(font).foregroundStyle(color)
                        .offset(x: scrolling ? -max(0, phase - 2) * 30 : 0)
                        .frame(width: proxy.size.width, alignment: scrolling ? .leading : .center)
                    }
                    .onChange(of: proxy.size.width) { start = .now }
                }.frame(height: lineHeight).clipped()
            }
        }
        .onChange(of: text) { start = .now }
        .accessibilityElement(children: .ignore).accessibilityLabel(text)
    }
}
