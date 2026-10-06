import SwiftUI

struct ChartNotesSection: View {
    let notes: CatalogSongViewData.Sheet.Notes

    var body: some View {
        let items = Array(zip(["TAP", "HOLD", "SLIDE", "AIR", "FLICK"], [notes.tap, notes.hold, notes.slide, notes.air, notes.flick]))
        let colors: [Color] = [.red, .orange, .blue, .green, .cyan]
        DetailDisclosure(title: tr("音符统计")) {
            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                    if let count = item.1 {
                        let fraction = Double(count) / Double(max(1, notes.total ?? items.compactMap(\.1).reduce(0, +)))
                        HStack(spacing: 8) {
                            Text(item.0).font(.system(size: 9, weight: .black)).foregroundStyle(.secondary).frame(width: 40, alignment: .leading)
                            GeometryReader { proxy in
                                Capsule().fill(.primary.opacity(0.06))
                                    .overlay(alignment: .leading) {
                                        Capsule().fill(colors[index].opacity(0.5))
                                            .frame(width: max(0, proxy.size.width * min(1, fraction)))
                                    }
                            }.frame(height: 6)
                            Text(count.formatted()).font(.system(size: 11, weight: .semibold, design: .monospaced)).frame(width: 44, alignment: .trailing)
                            Text(fraction, format: .percent.precision(.fractionLength(0)))
                                .font(.system(size: 9, weight: .medium, design: .monospaced)).foregroundStyle(.secondary).frame(width: 30, alignment: .trailing)
                        }.padding(.horizontal, 20).padding(.vertical, 8)
                            .background(index.isMultiple(of: 2) ? Color.primary.opacity(0.02) : .clear)
                    }
                }
            }
        }
    }
}
