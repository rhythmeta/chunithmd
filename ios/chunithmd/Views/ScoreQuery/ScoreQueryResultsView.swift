import Shared
import SwiftUI

struct ScoreQueryResultsView: View {
    let entries: [ScoreQueryEntry]
    let stats: ScoreQueryStats
    let songs: [String: CatalogSongViewData]
    let grid: Bool
    @AppStorage("scores.gridColumns") private var savedColumns = 5
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var zoom = 1.0
    @State private var initialZoom: Double?
    @State private var offset = 0.0
    @State private var viewport = 800.0
    @State private var width = 400.0
    @State private var headerHeight = 160.0
    @State private var anchorIndex = 0
    @State private var anchorY = 0.0
    @State private var scroll = ScrollPosition()

    private var geometry: CatalogGridGeometry { CatalogGridGeometry(width: width - 8, count: entries.count, zoom: zoom) }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                ScoreQueryStatsView(stats: stats)
                    .padding(.horizontal, 16).padding(.top, 8).padding(.bottom, 16)
                    .onGeometryChange(for: Double.self) { $0.size.height } action: { headerHeight = $0 }
                if entries.isEmpty {
                    ContentUnavailableView(tr("没有符合条件的成绩"), systemImage: "list.bullet.rectangle")
                } else if grid {
                    gridBody
                } else {
                    LazyVStack(spacing: 2) {
                        ForEach(entries, id: \.sheetKey) { entry in
                            ScoreQueryEntryView(entry: entry, song: songs[entry.songId])
                        }
                    }.padding(.horizontal, 12)
                }
            }.padding(.bottom, 20)
        }
        .scrollPosition($scroll)
        .scrollDismissesKeyboard(.interactively)
        .scrollDisabled(initialZoom != nil)
        .onScrollGeometryChange(for: Double.self) { $0.contentOffset.y + $0.contentInsets.top } action: { _, value in offset = max(0, value) }
        .onGeometryChange(for: CGSize.self) { $0.size } action: { width = $0.width; viewport = $0.height }
        .simultaneousGesture(grid && !entries.isEmpty ? pinch : nil)
        .onAppear { zoom = savedColumns == 3 ? 2 : 1 }
        .onChange(of: savedColumns) { if initialZoom == nil { zoom = savedColumns == 3 ? 2 : 1 } }
        .onChange(of: grid) { scroll.scrollTo(edge: .top) }
        .accessibilityIdentifier("score-query-results")
        .accessibilityAction(named: tr("放大封面")) { savedColumns = 3 }
        .accessibilityAction(named: tr("缩小封面")) { savedColumns = 5 }
    }

    private var gridBody: some View {
        ZStack(alignment: .topLeading) {
            Color.clear.frame(height: geometry.height)
            ForEach(geometry.visible(top: max(0, offset - headerHeight), height: viewport).map { (index: $0, entry: entries[$0]) }, id: \.entry.sheetKey) { item in
                let rect = geometry.frame(item.index)
                ScoreQueryEntryView(entry: item.entry, song: songs[item.entry.songId], grid: true,
                                    cornerRadius: geometry.columns == 3 ? 10 : 6)
                    .disabled(initialZoom != nil)
                    .frame(width: rect.width, height: rect.height)
                    .offset(x: rect.minX, y: rect.minY)
            }
        }
        .frame(height: geometry.height, alignment: .topLeading)
        .padding(.horizontal, 4)
    }

    private var pinch: some Gesture {
        MagnifyGesture().onChanged { value in
            if initialZoom == nil {
                initialZoom = zoom
                let focusY = max(0, offset + value.startAnchor.y * viewport - headerHeight)
                let column = min(geometry.columns - 1, max(0, Int(value.startAnchor.x * Double(geometry.columns))))
                anchorIndex = min(entries.count - 1, Int(focusY / geometry.step) * geometry.columns + column)
                anchorY = headerHeight + geometry.frame(anchorIndex).midY - offset
            }
            let base = CatalogGridGeometry(width: width - 8, count: entries.count, zoom: initialZoom ?? zoom)
            let desiredSize = base.size * value.magnification
            zoom = min(2, max(1, 1 + (desiredSize - base.sizeFor(5)) / (base.sizeFor(3) - base.sizeFor(5))))
            preserveAnchor()
        }.onEnded { _ in
            let target = zoom < 1.5 ? 1.0 : 2.0
            withAnimation(reduceMotion ? nil : .spring(duration: 0.3, bounce: 0)) {
                zoom = target
                preserveAnchor()
            }
            savedColumns = target == 1 ? 5 : 3
            initialZoom = nil
        }
    }

    private func preserveAnchor() {
        let maximum = max(0, headerHeight + geometry.height + 20 - viewport)
        scroll.scrollTo(y: max(0, min(maximum, headerHeight + geometry.frame(anchorIndex).midY - anchorY)))
    }
}
